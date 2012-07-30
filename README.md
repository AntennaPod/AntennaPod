AntennaPod
=========
This is the official repository of AntennaPod, a podcast manager for Android.

<a href="https://play.google.com/store/apps/details?id=de.danoeh.antennapod" alt="Download from Google Play">
  <img src="http://www.android.com/images/brand/android_app_on_play_large.png">
</a>

License
----------
AntennaPod is licensed under the MIT License. You can read the license text in the LICENSE file.

Dependencies
------------------
The AntennaPod app has the following dependencies:

- Android support library v4, already included in the libs directory
- [flattr4j](https://github.com/shred/flattr4j), already included in the libs directory
- [Apache Commons Lang](http://commons.apache.org/lang/), already included in the libs directory

- [ActionBarSherlock](https://github.com/JakeWharton/ActionBarSherlock)
- [ViewPagerIndicator](https://github.com/JakeWharton/Android-ViewPagerIndicator)
- Gridlayout from the support v7 library

ActionBarSherlock, ViewPagerIndicator and Gridlayout are not included in the repository. In order to build the app, you have to add them as a library project.
I am currently using Ant to build the project, but I think I am going to switch to Maven soon to make the build process more convenient.

Flattr API
------------
AntennaPod accesses the flattr API for flattring podcasts. In order to gain access, a client ID and a client secret is required, which you can get by registering a new app on the flattr website. The official API credentials have been excluded from the public source code.
In order to successfully build the project, a java class called FlattrConfig with two fields containing the credentials has to be created in src/de/danoeh/antennapod/util/flattr . You can also use the file called FlattrConfig.java.example to do that. If you leave the two fields blank, everything except the authentication process will work.