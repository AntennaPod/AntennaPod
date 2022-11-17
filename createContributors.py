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
config = configparser.ConfigParser()
config.read(os.path.expanduser("~") + '/.transifexrc')
if 'https://www.transifex.com' in config and config['https://www.transifex.com']['username'] == 'api':
    TRANSIFEX_TOKEN = config['https://www.transifex.com']['token']
else:
    TRANSIFEX_TOKEN = ""

languages = dict()

nextPage = 'https://rest.api.transifex.com/team_memberships?filter[organization]=o:antennapod'
while nextPage is not None:
    print("Loading " + nextPage)
    r = requests.get(nextPage,
            headers={'Authorization': 'Bearer ' + TRANSIFEX_TOKEN,
                    'Accept': 'application/vnd.api+json'})
    for item in r.json()['data']:
        language = item['relationships']['language']['data']['id']
        user = item['relationships']['user']['data']['id']
        if not language in languages:
            langCode = language.replace('l:', '')
            try:
                langName = pycountry.languages.lookup(langCode).name
            except:
                try:
                    langName = pycountry.languages.lookup(
                        langCode.split('_')[0]).name + ' (' + langCode + ')'
                except:
                    langName = code
                    print('\033[91mLanguage code not found:' + langCode + '\033[0m')
            languages[language] = {'name': langName, 'translators': []}
        languages[language]['translators'].append(user.replace('u:', ''))
    nextPage = r.json()['links']['next']

languages = list(languages.values())
languages.sort(key=lambda x : x['name'].lower())

csvFile = open("app/src/main/assets/translators.csv", "w")
contributorsFile.write('\n\n# Translators\n\n')
contributorsFile.write('| Language | Translators |\n| :-- | :-- |\n')
for language in languages:
    translators = sorted(language['translators'], key=str.lower)
    langName = language['name']
    joinedTranslators = ', '.join(translators).replace(';', '')
    contributorsFile.write('| ' + langName + ' | ' + joinedTranslators + ' |\n')
    csvFile.write(langName + ';' + joinedTranslators + '\n')
csvFile.close()
contributorsFile.close()

print('Done')

