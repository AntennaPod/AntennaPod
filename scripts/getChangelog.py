#!/usr/bin/env python3

import csv
import getpass
import json
import os
import sys
from datetime import datetime

import requests
import time

GRAPHQL_URL = "https://api.github.com/graphql"
OWNER = os.getenv("OWNER", "AntennaPod")
REPO = os.getenv("REPO", "AntennaPod")
TOKEN = os.getenv("GITHUB_API_TOKEN")
BASE = os.getenv("BASE", "")
HEAD = os.getenv("HEAD", "")
NEEDS_REVIEW_REPLY_LABEL = os.getenv("NEED_REVIEW_REPLY_LABEL", "Needs: Review reply")
PLAY_REVIEW_LINK_MARKERS = (
    "https://play.google.com/apps/publish?account=8008695526664634386#ReviewDetailsPlace:p=de.danoeh.antennapod&reviewid=",
    "https://play.google.com/console/u/0/developers/8008695526664634386/app/4974638472012894302/user-feedback/review-details?reviewId=",
)


def print_exception(prefix, error_message, data=None):
    print("\n" + prefix + ": " + str(error_message))
    if data:
        print(json.dumps(data, indent=2))


def graphql_request(query, variables):
    response = requests.post(
        GRAPHQL_URL,
        headers={"Authorization": "Bearer " + TOKEN},
        json={"query": query, "variables": variables},
        timeout=60,
    )
    response.raise_for_status()
    data = response.json()
    if "errors" in data:
        raise RuntimeError(json.dumps(data["errors"]))
    return data["data"]


def ask_token():
    token = TOKEN
    while not token:
        token = getpass.getpass(prompt="Token: ").strip()
    return token


def get_latest_release_tag():
    query = """
    query($owner: String!, $repo: String!) {
      repository(owner: $owner, name: $repo) {
        latestRelease {
          tagName
        }
      }
    }
    """
    data = graphql_request(query, {"owner": OWNER, "repo": REPO})
    return data["repository"]["latestRelease"]["tagName"]


def resolve_ref_name(ref_name):
    if ref_name.startswith("refs/"):
        return ref_name

    query = """
    query($owner: String!, $repo: String!, $name: String!) {
      repository(owner: $owner, name: $repo) {
        ref(qualifiedName: $name) {
          name
        }
      }
    }
    """
    candidates = [ref_name, "refs/heads/" + ref_name, "refs/tags/" + ref_name]
    for candidate in candidates:
        try:
            data = graphql_request(query, {"owner": OWNER, "repo": REPO, "name": candidate})
            if data["repository"]["ref"] is not None:
                return candidate
        except Exception:
            continue
    return ref_name


def read_base_ref():
    base = BASE
    if not base:
        print()
        print("Now, what should be our point of reference?")
        print("Please enter a release code or branch")
        print("[default: latest GitHub release]")
        base = input("Base: ").strip()
    if not base or base == "latest":
        return get_latest_release_tag()
    return base


def read_head_ref():
    head = HEAD
    if not head:
        print()
        print("Then, what should be our end reference?")
        print("Please enter a release code or branch")
        print("[default: 'master']")
        head = input("Head: ").strip()
    if not head:
        head = "master"
        print("Righty, master it is!")
        return head
    return head


def load_compare_commits(base_ref, head_ref):
    query = """
    query($owner: String!, $repo: String!, $baseRef: String!, $headRef: String!, $cursor: String) {
      repository(name: $repo, owner: $owner) {
        ref(qualifiedName: $baseRef) {
          compare(headRef: $headRef) {
            commits(first: 100, after: $cursor) {
              pageInfo {
                hasNextPage
                endCursor
              }
              nodes {
                oid
                committedDate
                messageHeadline
                url
                                authors(first: 100) {
                                    nodes {
                                        user {
                                            login
                                        }
                                    }
                                }
                author {
                  name
                  user {
                    login
                  }
                }
                associatedPullRequests(first: 1) {
                  nodes {
                    number
                  }
                }
              }
            }
          }
        }
      }
    }
    """

    all_commits = []
    cursor = None
    while True:
        data = graphql_request(
            query,
            {
                "owner": OWNER,
                "repo": REPO,
                "baseRef": base_ref,
                "headRef": head_ref,
                "cursor": cursor,
            },
        )
        page = data["repository"]["ref"]["compare"]["commits"]
        all_commits.extend(page["nodes"])
        if not page["pageInfo"]["hasNextPage"]:
            break
        cursor = page["pageInfo"]["endCursor"]
        time.sleep(0.5)
    return all_commits


def load_pr(pr_number):
    query = """
    query($owner: String!, $repo: String!, $prNumber: Int!) {
      repository(name: $repo, owner: $owner) {
        pullRequest(number: $prNumber) {
          number
          title
          url
          mergedAt
          author {
            login
          }
          labels(first: 100) {
            nodes {
              name
            }
          }
          closingIssuesReferences(first: 100) {
            nodes {
              number
              labels(first: 100) {
                nodes {
                  name
                }
              }
            }
          }
        }
      }
    }
    """
    data = graphql_request(query, {"owner": OWNER, "repo": REPO, "prNumber": pr_number})
    return data["repository"]["pullRequest"]


def comment_contains_review_link(comment_body):
    for marker in PLAY_REVIEW_LINK_MARKERS:
        if marker in comment_body:
            return True
    return False


def load_issue_review_reply_comment_urls(issue_number):
    query = """
    query($owner: String!, $repo: String!, $issueNumber: Int!, $cursor: String) {
      repository(name: $repo, owner: $owner) {
        issue(number: $issueNumber) {
          comments(first: 100, after: $cursor) {
            pageInfo {
              hasNextPage
              endCursor
            }
            nodes {
              body
              url
            }
          }
        }
      }
    }
    """

    urls = []
    cursor = None
    while True:
        data = graphql_request(
            query,
            {
                "owner": OWNER,
                "repo": REPO,
                "issueNumber": issue_number,
                "cursor": cursor,
            },
        )
        comments = data["repository"]["issue"]["comments"]
        for comment in comments["nodes"]:
            if comment_contains_review_link(comment["body"]):
                urls.append(comment["url"])
        if not comments["pageInfo"]["hasNextPage"]:
            break
        cursor = comments["pageInfo"]["endCursor"]
        time.sleep(0.5)
    return urls


def collect_review_reply_details(pr_data, review_reply_comment_cache):
    details = []
    numbers = []
    for issue in pr_data["closingIssuesReferences"]["nodes"]:
        issue_label_names = [node["name"] for node in issue["labels"]["nodes"]]
        if NEEDS_REVIEW_REPLY_LABEL not in issue_label_names:
            continue
        issue_number = issue["number"]
        numbers.append(issue_number)
        if issue_number not in review_reply_comment_cache:
            review_reply_comment_cache[issue_number] = load_issue_review_reply_comment_urls(issue_number)
        comment_urls = review_reply_comment_cache[issue_number]
        if comment_urls:
            details.append(str(issue_number) + ": " + " ".join(comment_urls))
        else:
            details.append(str(issue_number))
    return numbers, details


def collect_commit_author_logins(commit):
    logins = []
    for author in commit["authors"]["nodes"]:
        if author["user"] and author["user"]["login"]:
            logins.append(author["user"]["login"])
    if not logins and commit["author"] and commit["author"]["user"]:
        if commit["author"]["user"]["login"]:
            logins.append(commit["author"]["user"]["login"])
    return sorted(set(logins))


def main():
    global TOKEN
    print("Hello, welcome to the AntennaPod PR list generator!")
    print("First, please enter your GitHub API token.")
    print("If you don't have one yet, create it at https://github.com/settings/tokens")

    TOKEN = ask_token()
    try:
        _ = graphql_request("query { viewer { login } }", {})
    except Exception as error:
        print_exception("Error", "Invalid GitHub API token", {"details": str(error)})
        sys.exit(1)

    try:
        base_name = read_base_ref()
        head_name = read_head_ref()
        base_ref = resolve_ref_name(base_name)
        head_ref = resolve_ref_name(head_name)
    except Exception as error:
        print_exception("Error", error)
        sys.exit(1)

    print()
    print("Comparing " + base_name + "..." + head_name)

    try:
        commits = load_compare_commits(base_ref, head_ref)
    except Exception as error:
        print_exception("Error", "Could not load commits", {"details": str(error)})
        sys.exit(1)

    pr_numbers = []
    pr_authors = {}
    commit_rows = []
    for commit in commits:
        commit_authors = collect_commit_author_logins(commit)
        associated = commit["associatedPullRequests"]["nodes"]
        if associated:
            pr_number = associated[0]["number"]
            pr_numbers.append(pr_number)
            if pr_number not in pr_authors:
                pr_authors[pr_number] = set()
            for author_login in commit_authors:
                pr_authors[pr_number].add(author_login)
        else:
            author_login = " ".join(commit_authors)
            commit_rows.append(
                {
                    "entitytype": "commit",
                    "datetime": datetime.strptime(commit["committedDate"], "%Y-%m-%dT%H:%M:%SZ"),
                    "URL": commit["url"],
                    "title": commit["messageHeadline"],
                    "author": author_login,
                    "relatedIssues": "",
                    "labels": "",
                    "needsReviewReplies": "No",
                }
            )

    unique_pr_numbers = sorted(set(pr_numbers))
    pr_rows = []
    issues_needing_review_replies = set()
    review_reply_comment_cache = {}

    for index, pr_number in enumerate(unique_pr_numbers, start=1):
        print("PR " + str(index) + " of " + str(len(unique_pr_numbers)))
        try:
            pr_data = load_pr(pr_number)
        except Exception as error:
            print_exception("Error", "Could not load PR #" + str(pr_number), {"details": str(error)})
            sys.exit(1)

        related_issue_numbers = [str(issue["number"]) for issue in pr_data["closingIssuesReferences"]["nodes"]]
        related_issues = ", ".join(related_issue_numbers)

        label_names = set(node["name"] for node in pr_data["labels"]["nodes"])
        for issue in pr_data["closingIssuesReferences"]["nodes"]:
            for label in issue["labels"]["nodes"]:
                label_names.add(label["name"])
        labels = ", ".join(sorted(label_names))

        needs_review_reply_numbers, needs_review_reply_details = collect_review_reply_details(
            pr_data, review_reply_comment_cache
        )
        for number in needs_review_reply_numbers:
            issues_needing_review_replies.add(number)
        if needs_review_reply_details:
            needs_review_replies = "Yes (" + "; ".join(needs_review_reply_details) + ")"
        else:
            needs_review_replies = "No"

        pr_rows.append(
            {
                "entitytype": "pr",
                "datetime": datetime.strptime(pr_data["mergedAt"], "%Y-%m-%dT%H:%M:%SZ"),
                "URL": pr_data["url"],
                "title": pr_data["title"],
                "author": " ".join(sorted(pr_authors.get(pr_number, set()))),
                "relatedIssues": related_issues,
                "labels": labels,
                "needsReviewReplies": needs_review_replies,
            }
        )

    rows = sorted(commit_rows + pr_rows, key=lambda row: row["datetime"])
    filename = base_name + " - " + head_name + " changelog.csv"
    with open(filename, "w", newline="", encoding="utf-8") as output_file:
        writer = csv.writer(output_file)
        writer.writerow(
            [
                "Entity",
                "Merge/Committed date",
                "URL",
                "Title",
                "Author(s)",
                "Type",
                "Functionality group",
                "Related issue(s)",
                "Related label(s)",
                "Needs review replies?",
            ]
        )
        for row in rows:
            writer.writerow(
                [
                    row["entitytype"],
                    row["datetime"].strftime("%Y-%m-%dT%H:%M:%SZ"),
                    row["URL"],
                    row["title"],
                    row["author"],
                    "",
                    "",
                    row["relatedIssues"],
                    row["labels"],
                    row["needsReviewReplies"],
                ]
            )

    github_output = os.getenv("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as output_file:
            output_file.write("filename=" + filename + "\n")
            output_file.write(
                "issues=" + " ".join(str(number) for number in sorted(issues_needing_review_replies)) + "\n"
            )

    print("The changelog has been saved as '" + filename + "'.")


if __name__ == "__main__":
    main()
