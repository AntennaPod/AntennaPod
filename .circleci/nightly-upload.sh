#!/bin/bash

# The token you get here seems to work just fine:
# https://dropbox.github.io/dropbox-api-v2-explorer/#files_list_folder
accessToken="$DROPBOX_TOKEN"

rev=`git rev-parse HEAD`
shortrev=${rev:0:8}
date=`date +%Y-%m-%d-%H-%M-%S`
nightlyFolder="/AntennaPodNightlies"
filename="$nightlyFolder/nightly-$date-$shortrev.apk"
localFilename="./app/build/outputs/apk/play/debug/app-play-debug.apk"
filesToKeep="6" # This needs to be one more than the number of files to actually keep

echo "Uploading apk to Dropbox..."

curl -X POST https://content.dropboxapi.com/2/files/upload \
    --header "Authorization: Bearer $accessToken" \
    --header "Dropbox-API-Arg: {\"path\": \"$filename\"}" \
    --header "Content-Type: application/octet-stream" \
    --data-binary @$localFilename \
    --progress-bar > /dev/null
    
echo "Checking old files..."

oldFilesToDelete=$(curl -s -X POST https://api.dropboxapi.com/2/files/list_folder \
  --header "Authorization: Bearer $accessToken" \
  --header "Content-Type: application/json" \
  --data "{\"path\":\"$nightlyFolder\"}" \
  | grep -Po '"name":.*?[^\\]",' | sed -E 's/"name":[ +]"(.*?)",/\1/g' \
  | sort -r | tail -n +$filesToKeep)
  
while read -r file; do
    if [ -n "$file" ]; then
        echo "Deleting old file: $file..."
        curl -s -X POST https://api.dropboxapi.com/2/files/delete \
            --header "Authorization: Bearer $accessToken" \
            --header "Content-Type: application/json" \
            --data "{\"path\":\"$nightlyFolder/$file\"}" > /dev/null
    fi
done <<< "$oldFilesToDelete"

echo "Done uploading nightly build."
