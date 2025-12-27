#!/usr/bin/env python3
import pycountry, requests, os, glob, subprocess

contributorsFile = open("CONTRIBUTORS.md", "w")

###### Developers #####
MAINTAINERS = ['ByteHamster', 'keunes']
FORMER_MAINTAINERS = ['mfietz', 'TomHennen']

csvFile = open("ui/preferences/src/main/assets/developers.csv", "w")
contributorsFile.write('# Developers\n\n')
page = 1
hasMore = True
first = True
while hasMore:
    nextPage = ('https://api.github.com/repos/AntennaPod/AntennaPod/contributors'
        + '?q=contributions&order=desc&per_page=100&page=' + str(page))
    print("Loading " + nextPage)
    json = requests.get(nextPage).json()
    for contributor in json:
        role = 'Contributor'
        if contributor['login'] == 'danieloeh':
            role = 'Original creator of AntennaPod (retired)'
        elif contributor['login'] in MAINTAINERS:
            role = 'Maintainer'
        elif contributor['login'] in FORMER_MAINTAINERS:
            role = 'Maintainer (retired)'
        csvFile.write(contributor['login'].replace(";", "") + ';' + str(contributor['id']) + ';' + role + '\n')
        if not first:
            contributorsFile.write(', ')
        first = False
        contributorsFile.write('[' + contributor['login'] + '](https://github.com/' + contributor['login'] + ')')
    page = page + 1
    hasMore = len(json) > 0
csvFile.close()

###### Translators #####

path = "ui/i18n/src/main/res"

# Map Android codes to display names when pycountry fails or region matters
ANDROID_LANG_FIXES = {
    "pt-rbr": "Portuguese (Brazil)",
    "pt-rpt": "Portuguese (Portugal)",
    "kn-rin": "Kannada (India)",
    "zh-rcn": "Chinese (Simplified)",
    "zh-rtw": "Chinese (Traditional)",
    "in": "Indonesian",
    "iw": "Hebrew",
    "ji": "Yiddish",
    "sw": "Swahili",
    "el": "Modern Greek",
}
def lang_name(code):
    code = code.lower()
    if code in ANDROID_LANG_FIXES:
        return ANDROID_LANG_FIXES[code]
    norm = code.replace('-r','_')
    try:
        base = norm.split('_')[0]
        name = pycountry.languages.lookup(base).name
        return name + (f" ({norm})" if '_' in norm else "")
    except:
        return code

files = sorted(
    glob.glob(f"{path}/values-*/strings.xml"),
    key=lambda f: lang_name(f.split("values-")[1].split("/")[0]).lower())
contributorsFile.write('\n\n# Translators\n\n| Language | Translators |\n| :-- | :-- |\n')
csvFile = open("ui/preferences/src/main/assets/translators.csv", "w")

for f in files:
    code = f.split("values-")[1].split("/")[0]
    hashes = {l.split()[0] for l in subprocess.check_output(["git", "blame", f]).decode().splitlines()}
    names = set()
    for h in hashes:
        for l in subprocess.check_output(["git", "show", h]).decode().splitlines():
            if not f"Translator: {code} by" in l:
                continue
            n = l.split("by",1)[1].split("<")[0].replace('"','').strip()
            if n in ("ByteHamster", "Anonymous"):
                continue
            names.add(n)
    translators = ", ".join(sorted(names)) or "Anonymous"
    lang = lang_name(code)
    contributorsFile.write(f"| {lang} | {translators} |\n")
    csvFile.write(f"{lang};{translators}\n")
    print(f"{lang}: {translators}")

csvFile.close()
contributorsFile.close()

print('Done')

