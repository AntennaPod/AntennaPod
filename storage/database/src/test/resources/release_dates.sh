#!/bin/bash

ids=$(curl --silent "https://itunes.apple.com/US/rss/toppodcasts/limit=200/explicit=true/json" | jq --raw-output ".feed.entry[].id.attributes.[\"im:id\"]")

echo "$ids" | while IFS= read -r id;
do
    sleep 2
    url=$(curl --silent "https://itunes.apple.com/lookup?id=$id" | jq --raw-output ".results[0].feedUrl")
    dates=$(curl --silent "$url" | yq --input-format xml ".rss.channel.item[].pubDate")
    numdates=$(echo -en "$dates" | wc -l)
    if [ $numdates -ge 5 ]; then
        echo -en "$dates" | head -n 50 | paste -sd ";"
    fi
done
