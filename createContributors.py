#!/usr/bin/env python3
import pycountry
import requests
import configparser
import os

contributorsFile = open("CONTRIBUTORS.md", "w")

###### Developers #####
MAINTAINERS = ['ByteHamster', 'keunes']
FORMER_MAINTAINERS = ['mfietz', 'TomHennen']

csvFile = open("app/src/main/assets/developers.csv", "w")
contributorsFile.write('# Developers\n\n')
page = 1
hasMore = True
first = True
while hasMore:
    json = requests.get('https://api.github.com/repos/AntennaPod/AntennaPod/contributors'
        + '?q=contributions&order=desc&per_page=100&page=' + str(page)).json()
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
config = configparser.ConfigParser()
config.read(os.path.expanduser("~") + '/.transifexrc')
if 'https://www.transifex.com' in config:
    TRANSIFEX_USER = config['https://www.transifex.com']['username']
    TRANSIFEX_PW = config['https://www.transifex.com']['password']
else:
    TRANSIFEX_USER = ""
    TRANSIFEX_PW = ""

csvFile = open("app/src/main/assets/translators.csv", "w")
r = requests.get('http://www.transifex.com/api/2/project/antennapod/languages/',
        auth=(TRANSIFEX_USER, TRANSIFEX_PW))
contributorsFile.write('\n\n# Translators\n\n')
contributorsFile.write('| Language | Translators |\n| :-- | :-- |\n')

for lang in r.json():
    langContributers = lang['coordinators'] + lang['reviewers'] + lang['translators']
    langContributers = sorted(langContributers, key=str.lower)
    langCode = lang['language_code']
    try:
        langName = pycountry.languages.lookup(langCode).name
    except:
        try:
            langName = pycountry.languages.lookup(
                langCode.split('_')[0]).name + ' (' + langCode + ')'
        except:
            langName = lang['language_code']
            print('\033[91mLanguage code not found:' + langCode + '\033[0m')

    joinedTranslators = ', '.join(langContributers).replace(';', '')
    contributorsFile.write('| ' + langName + ' | ' + joinedTranslators + ' |\n')
    csvFile.write(langName + ';' + joinedTranslators + '\n')
csvFile.close()

contributorsFile.close()
