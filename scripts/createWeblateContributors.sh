#!/bin/bash

path=ui/i18n/src/main/res
for filename in $path/values-*/strings.xml; do
    code=$(echo "$filename" | sed -e "s#$path/values-##g" | sed -e "s#/.*##g")
    translators_all=$(git blame $path/values-$code/strings.xml | cut -d' ' -f 1 | sort | uniq | xargs -n 1 git show | grep "Translator: $code by" | sed -e "s/<.*>//g" | sed -e "s/Translator: $code by//g" | xargs -n 1 | sort | uniq)
    translators_filtered=$(printf "$translators_all" | grep -v "ByteHamster" | grep -v "Anonymous")
    translators_singleline=$(echo "$translators_filtered" | paste -sd ',')
    if [ -z "$translators_singleline" ]; then
        translators_singleline="Anonymous"
    fi
    echo "$code;$translators_singleline"
done
