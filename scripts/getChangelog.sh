#!/bin/sh

# TODO
# merge PR & commit lists into single ODS with 2 tabs
# add PR number(s) to each row in commits CSV
# add some kind of progress indicator when going through list of commits

REPO="AntennaPod/AntennaPod"

echo "Hello, welcome to the AntennaPod PR list generator!"
echo "First, please enter your GitHub API token."
echo "If you don't have one yet, create it at https://github.com/settings/tokens"
read -p "Token: " TOKEN
if [ -z "$TOKEN" ]; then
    exit 1
fi
echo "Grand, thank you! ($TOKEN is noted)"
sleep 1

echo ""
echo "Now, what do you want to compare?"
echo "Please enter a release code or branch"
echo "[default: latest GitHub release]"
read -p "Base: " BASE
if [ -z "$BASE" ]; then
    BASE=$(curl -s -H "Authorization: token $TOKEN" https://api.github.com/repos/$REPO/releases/latest | jq -r '.tag_name')
    echo "Okido, latest release ($BASE) it is!"
else
    echo "Noted."
fi
sleep 1

echo ""
echo "Then, what should be our endpoint?"
echo "[default: 'develop']"
read -p "Head: " HEAD
if [ -z "$HEAD" ]; then
    echo "Righty, develop it is!"
    HEAD=develop
else
    echo "Roger that."
fi
sleep 1

echo ""
echo "Ok, let's download a list of commits (minus merge commits)."
curl -s -H "Authorization: token $TOKEN" "https://api.github.com/repos/$REPO/compare/$BASE...$HEAD" | jq -r -e '.commits[] | select((.commit.message | contains("Merge pull request #") | not) and (.commit.message | contains("Merge branch") | not)) | [.sha, .commit.message, .author.login, .html_url] | @csv' > "$BASE-$HEAD commits.csv"
sed -i '1s/^/sha,message,author,URL,PR\n/' "$BASE-$HEAD commits.csv"
COMMITS=$(curl -s -H "Authorization: token $TOKEN" "https://api.github.com/repos/$REPO/compare/$BASE...$HEAD" | jq -r -e '.commits[] | select((.commit.message | contains("Merge pull request #") | not) and (.commit.message | contains("Merge branch") | not)) | .sha')
echo "Done."
COMMITSNUMBER=$(echo $COMMITS | wc -w)
sleep 1

echo ""
echo "Now we'll use that list of commits to ask GitHub in which PRs these commits ended up. We'll first create an empty CSV file. Then we'll add all the PRs to the list."
echo ""

rm "PR list $BASE-$HEAD.csv" 2>/dev/null

for COMMIT in $COMMITS
do
   echo "$COMMIT" started
   PRs=$(curl -s -H "Authorization: token $TOKEN" "https://api.github.com/search/issues?q=repo:$REPO+type:pr+is:merged+$COMMIT" | jq -r -e '.items[] | [.pull_request.merged_at, .html_url, .title, .user.login] | @csv')
   status=$?
   if [ $status -eq 4 ]; then
      OrphanCommits=true
      sed -i "/^\"$COMMIT\"/ s/$/,orphan/" "$BASE-$HEAD commits.csv"
      # This is problematic if a commit message has newlines in it, because then the orphan tag will be placed inside the message rather than at the end of the row
      echo "Commit $COMMIT doesn't appear in any PR! It's marked as orphan in the commit list."
   elif [ $status -ne 0 ]; then
      echo "Error parsing JSON response with the PRs for commit"
      echo "Here is the JSON response that's causing the error (also stored in $COMMIT-PRS.json:"
      curl -s -H "Authorization: token $TOKEN" "https://api.github.com/search/issues?q=repo:$REPO+type:pr+is:merged+$COMMIT" > "$COMMIT-PRs.json"
      cat "$COMMIT-PRs.json"
      exit 1
   fi
   echo "$PRs"  >> "PR list $BASE-$HEAD.csv"
   sleep 2 # Otherwise we'll hit GitHub's rate limit
done

echo "Removing duplicates..."
sort "PR list $BASE-$HEAD.csv" | uniq > temp.csv && mv temp.csv "PR list $BASE-$HEAD.csv"
sed -i '/^$/d' "PR list $BASE-$HEAD.csv"
sed -i '1s/^/PR Merge date,PR URL,PR Title,PR Author,type,functionaliy group\n/' "PR list $BASE-$HEAD.csv"

echo "Done."
rm PRs-JSON.json 2>/dev/null

echo ""
echo "The PRs of all commits were successfully identified!"
if [ "$OrphanCommits" = true ]; then
   echo ""
   echo "There are a bunch of commits that aren't linked to any PR. We have marked them in the commits file."
fi
echo ""
