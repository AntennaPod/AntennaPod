#!/bin/sh

function generateText() {
    echo "$1"
    convert -size 1698x750 xc:none -gravity Center -pointsize 150 -fill white -font Lato-Regular \
        -annotate 0 "$1" /tmp/text.png
}

function simplePhone() {
    generateText "$1"
    convert templates/phone.png \
        $2 -geometry +306+989 -composite \
        /tmp/text.png -geometry +0+0 -composite \
        $3
}

function addLayer() {
    convert $2 $1 -composite $2
}

function generateScreenshots() {
    language=$1
    mkdir output/$1 2>/dev/null
    text0=`cat raw/$language/texts.txt | head -1 | tail -1`
    text1=`cat raw/$language/texts.txt | head -2 | tail -1`
    text2=`cat raw/$language/texts.txt | head -3 | tail -1`
    text3=`cat raw/$language/texts.txt | head -4 | tail -1`
    text4=`cat raw/$language/texts.txt | head -5 | tail -1`
    text5=`cat raw/$language/texts.txt | head -6 | tail -1`

    simplePhone "$text0" raw/$language/00.png output/$language/00.png
    simplePhone "$text1" raw/$language/01.png output/$language/01.png
    simplePhone "$text2" raw/$language/02.png output/$language/02.png
    generateText "$text3"
    convert templates/twophones.png \
        templates/twophones-a.png -geometry +0+0 -composite \
        raw/$language/03a.png -geometry +119+984 -composite \
        templates/twophones-b.png -geometry +0+0 -composite \
        raw/$language/03b.png -geometry +479+1540 -composite \
        /tmp/text.png -geometry +0+0 -composite \
        output/$language/03.png
    simplePhone "$text4" raw/$language/04.png output/$language/04.png
    simplePhone "$text5" raw/$language/05.png output/$language/05.png
    addLayer templates/suggestions.png output/$language/05.png
    optipng output/$language/*.png
}

mkdir output 2>/dev/null

generateScreenshots "de-DE"
generateScreenshots "en-US"

rm /tmp/text.png
