#!/usr/bin/env python3

import requests
import time

REPO = "AntennaPod/AntennaPod"

print("Hello, welcome to the AntennaPod PR list generator!")
print("First, please enter your GitHub API token.")
print("If you don't have one yet, create it at https://github.com/settings/tokens")

def get_token():
    TOKEN = ""
    while not TOKEN:
        TOKEN = input('Token: ').strip()
    print("Grand, thank you! (" + TOKEN + " is noted)")
    return TOKEN

TOKEN = get_token()

print()
print("Now, what do you want to compare?")
print("Please enter a release code or branch")
print("[default: latest GitHub release]")
BASE = input('Base: ')
if BASE == "":
    response = requests.get("https://api.github.com/repos/" + REPO + "/releases/latest", headers={'Authorization': 'token ' + TOKEN})
    while response.status_code == 401:
        print("Error: Invalid GitHub API token.")
        TOKEN = get_token()
        response = requests.get("https://api.github.com/repos/" + REPO + "/releases/latest", headers={'Authorization': 'token ' + TOKEN})
    release = response.json()
    BASE = release["tag_name"]
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
filename = BASE + " - " + HEAD + "changelog.csv"
outputFile = open(filename, 'w')
outputFile.write("Type,Merge date,URL,Title,Author,Type,Functionality group\n")
commits = requests.get("https://api.github.com/repos/" + REPO + "/compare/" + BASE + "..." + HEAD, headers={'Authorization': 'token ' + TOKEN}).json()
numCommits = len(commits["commits"])
for i in range(numCommits):
    sha = commits["commits"][i]["sha"]
    commit = commits["commits"][i]
    print("Commit " + sha + " (" + str(i+1) + " of " + str(numCommits) + ")")
    if "Merge pull request #" in commit["commit"]["message"] or "Merge branch" in commit["commit"]["message"]:
        print("  [is merge commit]")
        continue
    time.sleep(2) # Avoid rate limit
    try:
        prs = requests.get("https://api.github.com/search/issues?q=repo:" + REPO + "+type:pr+is:merged+" + sha, headers={'Authorization': 'token ' + TOKEN}).json()
        if len(prs["items"]) == 0:
            print("  [orphan] " + commit["commit"]["message"].splitlines()[0])
            raise Exception('Results')
        firstPr = prs["items"][0]
        if firstPr["number"] in prsSeen:
            print("  [already seen] " + firstPr["title"] + " (#" + str(firstPr["number"]) + ")")
            continue
        outputFile.write("PR," + firstPr["pull_request"]["merged_at"] + "," + firstPr["html_url"] + ",\"" + firstPr["title"] + "\"," + firstPr["user"]["login"] + "\n")
        print("  " + firstPr["title"] + " (#" + str(firstPr["number"]) + ")")
        prsSeen.add(firstPr["number"])
    except Exception as e:
        print("  [exception] " + commit["commit"]["message"].splitlines()[0])
        outputFile.write("Commit," + commit["commit"]["committer"]["date"] + "," + commit["html_url"] + ",\"" + commit["commit"]["message"].splitlines()[0] + "\"," + commit["committer"]["login"] + "\n")
outputFile.close()
