#!/usr/bin/env python3
import pycountry
import requests
import configparser
import os

config = configparser.ConfigParser()
config.read(os.path.expanduser("~") + '/.transifexrc')
if 'https://www.transifex.com' in config:
    TRANSIFEX_USER = config['https://www.transifex.com']['username']
    TRANSIFEX_PW = config['https://www.transifex.com']['password']
else:
    TRANSIFEX_USER = ""
    TRANSIFEX_PW = ""

csvFile = open("app/src/main/assets/translators.csv", "w")
contributorsFile = open("CONTRIBUTORS", "a")
r = requests.get('http://www.transifex.com/api/2/project/antennapod/languages/',
        auth=(TRANSIFEX_USER, TRANSIFEX_PW))
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
    contributorsFile.write(langName + ": " + joinedTranslators + '\n')
    csvFile.write(langName + ';' + joinedTranslators + '\n')
    print(langName + ';' + joinedTranslators)
csvFile.close()
contributorsFile.close()
