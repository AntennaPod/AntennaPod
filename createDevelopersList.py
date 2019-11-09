#!/usr/bin/env python3
import requests

MAINTAINERS = ['mfietz', 'ByteHamster']
FORMER_MAINTAINERS = ['TomHennen']

csvFile = open("app/src/main/assets/developers.csv", "w")
contributorsFile = open("CONTRIBUTORS", "a")
page = 1
hasMore = True
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
        line = contributor['login'].replace(";", "") + ';' + str(contributor['id']) + ';' + role
        csvFile.write(line + '\n')
        print(line)
        contributorsFile.write(contributor['login'] + '\n')
    page = page + 1
    hasMore = len(json) > 0
csvFile.close()
contributorsFile.close()
         
