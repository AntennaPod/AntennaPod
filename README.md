# AntennaPod

This is the official repository of AntennaPod, a podcast manager for Android.


<a href="https://play.google.com/store/apps/details?id=de.danoeh.antennapod" alt="Download from Google Play">
  <img src="http://www.android.com/images/brand/android_app_on_play_large.png">
</a>
[AntennaPod on fdroid.org](http://f-droid.org/repository/browse/?fdcategory=Multimedia&fdid=de.danoeh.antennapod&fdpage=1)

## License

AntennaPod is licensed under the MIT License. You can find the license text in the LICENSE file.

## Translating AntennaPod
If you want to translate AntennaPod into another language, you can visit the [Transifex project page](https://www.transifex.com/projects/p/antennapod/).


## Dependencies

AntennaPod has the following dependencies:

- [flattr4j](http://www.shredzone.org/projects/flattr4j/files)
- [Apache Commons Lang](http://commons.apache.org/lang/download_lang.cgi)
- [ActionBarSherlock](https://github.com/JakeWharton/ActionBarSherlock)
- [ViewPagerIndicator](https://github.com/JakeWharton/Android-ViewPagerIndicator)
- [Apache Commons IO](http://commons.apache.org/io/download_io.cgi)

## Building

Before building, make sure you have added FlattrConfig.java as described in the 'Flattr API' - section!

### Building with ant

ActionBarSherlock and ViewPagerIndicator have to be added as library projects. Flattr4j, Apache Commons Lang and Apache Commons IO are jar-libraries and have to be copied into the libs folder in the root directory. 

### Building with maven

You can already build unsigned packages with maven, if you add annotations.jar from the Android SDK to your local maven repository. You don't have to do anything described in the 'Building with ant' section in order to build with maven.

- Make sure the ANDROID_HOME variable is set to the location of your Android SDK installation
- Navigate from your Android SDK directory into tools/support
- Execute the following command:
	<pre>
mvn install:install-file -Dfile=./annotations.jar -DgroupId=android.tools.support -DartifactId=annotations -Dversion=1.0 -Dpackaging=jar
</pre>
- In the root directory of this project, you can then execute the following command to build it:	
	<pre>mvn clean package</pre>

## Flattr API

AntennaPod accesses the flattr API for flattring podcasts. In order to gain access, a client ID and a client secret is required, which you can get by registering a new app on the flattr website. The official API credentials have been excluded from the public source code.
In order to successfully build the project, a java class called FlattrConfig with two fields containing the credentials has to be created in src/de/danoeh/antennapod/util/flattr . You can also use the file called FlattrConfig.java.example to do that. If you leave the two fields blank, everything except the authentication process will work.

## Donate

[![Flattr Button](http://api.flattr.com/button/button-static-50x60.png "Flattr This!")](https://flattr.com/thing/745609/Antennapod "AntennaPod")

Bitcoin donations can be sent to this address: <pre>1DzvtuvdW8VhDsq9GUytMyALmsHeaHEKbg</pre>

