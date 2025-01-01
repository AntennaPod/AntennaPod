#!/bin/sh

englishLenght=$(cat ui/i18n/src/main/res/values/strings.xml | wc -l)

languagesTranslatedXml="<locale android:name=\"en\"/>"
languagesTranslated="\"en\""
languagesNotEnough=""

for folder in ui/i18n/src/main/res/values-* ; do
    language="${folder:28}"
    lenght=$(cat "$folder/strings.xml" | wc -l)
    length="$lenght"
    percentageTranslated=$(( (100 * length) / englishLenght ))
    if [ "$percentageTranslated" -gt "40" ]; then
        languagesTranslatedXml="$languagesTranslatedXml\n<locale android:name=\"$language\"/>"
        languagesTranslated="$languagesTranslated, \"$language\""
    else
        languagesNotEnough="$languagesNotEnough, $language"
    fi
done

echo -e "$languagesTranslatedXml"
echo -e "\n\n"
echo "At least 40% translated: $languagesTranslated"
echo "Less than 40% translated: $languagesNotEnough"

