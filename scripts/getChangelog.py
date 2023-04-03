#!/usr/bin/env python3
import requests
import time

REPO = "AntennaPod/AntennaPod"

print("Hello, welcome to the AntennaPod PR list generator!")
print("First, please enter your GitHub API token.")
print("If you don't have one yet, create it at https://github.com/settings/tokens")
TOKEN = input('Token: ')
print("Grand, thank you! (" + TOKEN + " is noted)")

print()
print("Now, what do you want to compare?")
print("Please enter a release code or branch")
print("[default: latest GitHub release]")
BASE = input('Base: ')
if BASE == "":
    release = requests.get("https://api.github.com/repos/" + REPO + "/releases/latest", headers={'Authorization': 'token ' + TOKEN}).json()
    BASE=release["tag_name"]
    print("Okido, latest release (" + BASE + ") it is!")
else:
    print("Noted")

print()
print("Then, what should be our endpoint?")
print("[default: 'master']")
HEAD = input('Head: ')
if HEAD == "":
    print("Righty, master it is!")
    HEAD="master"
else:
    print("Roger that.")

print()
prsSeen = set()
outputFile = open('PRs.csv', 'w')
outputFile.write("PR Merge date,PR URL,PR Title,PR Author,type,functionaliy group\n")
commits = requests.get("https://api.github.com/repos/" + REPO + "/compare/" + BASE + "..." + HEAD, headers={'Authorization': 'token ' + TOKEN}).json()
numCommits = len(commits["commits"])
for i in range(numCommits):
    print("Commit " + str(i) + " of " + str(numCommits))
    sha = commits["commits"][i]["sha"]
    commit = commits["commits"][i]["commit"]
    if "Merge pull request #" in commit["message"] or "Merge branch" in commit["message"]:
        print("  Is merge")
        continue
    time.sleep(1) # Avoid rate limit
    try:
        prs = requests.get("https://api.github.com/search/issues?q=repo:" + REPO + "+type:pr+is:merged+" + sha, headers={'Authorization': 'token ' + TOKEN}).json()
        if len(prs["items"]) == 0:
            print("  No search results: " + commit["message"].splitlines()[0])
            raise Exception('Results')
        firstPr = prs["items"][0]
        if firstPr["number"] in prsSeen:
            print("  Already seen: " + firstPr["title"])
            continue
        outputFile.write(firstPr["pull_request"]["merged_at"] + "," + firstPr["html_url"] + "," + firstPr["title"] + "," + firstPr["user"]["login"] + "\n")
        print("  " + firstPr["title"])
        prsSeen.add(firstPr["number"])
    except Exception as e:
        print("  Orphan: " + commit["message"].splitlines()[0])
        outputFile.write("Orphan,Orphan," + commit["message"].splitlines()[0] + "," + commit["author"].get("login", "Unknown author") + "\n")
outputFile.close()
