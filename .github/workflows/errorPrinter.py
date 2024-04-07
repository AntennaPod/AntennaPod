#!/bin/python
from xml.dom import minidom
import os.path
import glob
from pathlib import Path

if os.path.isfile('app/build/reports/lint-results-playDebug.xml'):
    dom = minidom.parse('app/build/reports/lint-results-playDebug.xml')
    issues = dom.getElementsByTagName('issue')
    for issue in issues:
        locations = issue.getElementsByTagName('location')
        for location in locations:
            print(location.attributes['file'].value + ":" + location.attributes['line'].value + " " + issue.attributes['summary'].value)
            print("::error file=" + location.attributes['file'].value
                + ",line=" + location.attributes['line'].value
                + ",endLine=" + location.attributes['line'].value
                + ",title=Lint::" + issue.attributes['summary'].value + ". " + issue.attributes['explanation'].value.replace('\n', ' '))
            print()

if os.path.isfile('build/reports/checkstyle/checkstyle.xml'):
    dom = minidom.parse('build/reports/checkstyle/checkstyle.xml')
    files = dom.getElementsByTagName('file')
    for f in files:
        errors = f.getElementsByTagName('error')
        for error in errors:
            print(f.attributes['name'].value + ":" + error.attributes['line'].value + " " + error.attributes['message'].value)
            print("::error file=" + f.attributes['name'].value
                + ",line=" + error.attributes['line'].value
                + ",endLine=" + error.attributes['line'].value
                + ",title=Checkstyle::" + error.attributes['message'].value)
            print()


for filename in glob.iglob('**/build/reports/spotbugs/*.xml', recursive=True):
    filenamePath = Path(filename)
    dom = minidom.parse(filename)
    instance = dom.getElementsByTagName('BugInstance')
    for inst in instance:
        lines = inst.getElementsByTagName('SourceLine')
        longMessage = inst.getElementsByTagName('LongMessage')[0].firstChild.nodeValue
        for line in lines:
            if "primary" in line.attributes:
                print(line.attributes['sourcepath'].value + ": " + longMessage)
                print("::error file=" + str(filenamePath.parent.parent.parent.parent.absolute()) + "/src/main/java/" + line.attributes['sourcepath'].value
                    + ",line=" + line.attributes['start'].value
                    + ",endLine=" + line.attributes['end'].value
                    + ",title=SpotBugs::" + longMessage.replace('\n', ' '))
                print()
