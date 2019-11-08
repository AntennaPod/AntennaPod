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

csvFile = open("app/src/main/assets/translators.txt", "w")
r = requests.get('http://www.transifex.com/api/2/project/antennapod/languages/',
        auth=(TRANSIFEX_USER, TRANSIFEX_PW))
for lang in r.json():
    lang_contributers = lang['coordinators'] + lang['reviewers'] + lang['translators']
    lang_contributers = sorted(lang_contributers, key=str.lower)
    lang_code = lang['language_code']
    try:
        lang_name = pycountry.languages.lookup(lang_code).name
    except:
        try:
            lang_name = pycountry.languages.lookup(
                lang_code.split('_')[0]).name + ' (' + lang_code + ')'
        except:
            lang_name = lang['language_code']
            print('\033[91mLanguage code not found:' + lang_code + '\033[0m')

    line = lang_name + ';' + ', '.join(lang_contributers).replace(';', '')
    csvFile.write(line + '\n')
    print(line)
csvFile.close()
