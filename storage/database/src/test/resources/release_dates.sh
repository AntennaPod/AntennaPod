#!/bin/bash

ids=$(curl --silent "https://itunes.apple.com/US/rss/toppodcasts/limit=150/explicit=true/json" | jq --raw-output ".feed.entry[].id.attributes.[\"im:id\"]")

echo "$ids" | while IFS= read -r id;
do
    url=$(curl --silent "https://itunes.apple.com/lookup?id=$id" | jq --raw-output ".results[0].feedUrl")
    dates=$(curl --silent "$url" | yq --input-format xml ".rss.channel.item[].pubDate")
    echo -en "$dates" | head -n 50 | paste -sd ";"
done
