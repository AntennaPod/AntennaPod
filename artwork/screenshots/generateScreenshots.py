import os
from pathlib import Path

def generateText(text, font):
    print(text)
    os.system('convert -size 1698x750 xc:none -gravity Center -pointsize 130 -fill white -font ' + font + ' -annotate 0 "' + text + '" /tmp/text.png')

def simplePhone(text, screenshotFile, outputFile, font):
    generateText(text, font)
    os.system('convert templates/phone.png ' + screenshotFile + ' -geometry +306+992 -composite /tmp/text.png -geometry +0+0 -composite ' + outputFile + '')

def twoPhones(text, rawScreenshotsPath, outputFile, font):
    generateText(text, font)
    os.system('convert templates/twophones.png '
        + 'templates/twophones-a.png -geometry +0+10 -composite '
        + rawScreenshotsPath + '/03a.png -geometry +119+992 -composite '
        + 'templates/twophones-b.png -geometry +0+0 -composite '
        + rawScreenshotsPath + '/03b.png -geometry +479+1540 -composite '
        + '/tmp/text.png -geometry +0+0 -composite '
        + outputFile)

def generateScreenshots(language, font):
    Path('output/' + language).mkdir(parents=True, exist_ok=True)
    with open('raw/' + language + '/texts.txt') as textDefinitions:
        texts = textDefinitions.readlines()
    rawScreenshotsPath = 'raw/' + language
    outputPath = 'output/' + language
    
    if not Path(rawScreenshotsPath + '/00.png').is_file():
        rawScreenshotsPath = 'raw/en-US'

    simplePhone(texts[0], rawScreenshotsPath + '/00.png', outputPath + '/00.png', font)
    simplePhone(texts[1], rawScreenshotsPath + '/01.png', outputPath + '/01.png', font)
    simplePhone(texts[2], rawScreenshotsPath + '/02.png', outputPath + '/03.png', font)
    twoPhones(texts[3], rawScreenshotsPath, outputPath + '/02.png', font)
    simplePhone(texts[4], rawScreenshotsPath + '/04.png', outputPath + '/04.png', font)
    simplePhone(texts[5], rawScreenshotsPath + '/05.png', outputPath + '/05.png', font)
    os.system('mogrify -resize 1120 "' + outputPath + '/*.png"')

generateScreenshots("de-DE", "Sarabun-Regular")
generateScreenshots("en-US", "Sarabun-Regular")
generateScreenshots("fr-FR", "Sarabun-Regular")
generateScreenshots("he-IL", "Arimo-Regular")
generateScreenshots("nl-NL", "Sarabun-Regular")
generateScreenshots("it-IT", "Sarabun-Regular")
generateScreenshots("es-ES", "Sarabun-Regular")
