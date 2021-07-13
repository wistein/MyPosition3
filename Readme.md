# MyPosition3
 
## My Position (Mein Standort)
 
Share your location, easily.  
The app simplifies the task of sharing your location data with your contacts.
### Features
- My Position determines the current location with address data and GPS coordinates including height above sea-level. 
- Your location may be shown on a map either local (if a suitable app is installed and the default setting of Google Maps is changed*) or by browser on OpenStreetMap.
- It uses Reverse Geocoding by OpenStreetMap (OSM) query for showing address info.**)
- It shows a country-specific representation of address info (at, ch, de, fr, it, rest of the world), introduces localized strings and number formats (German, English).
- Additionally, the app also includes a tool to help converting between WGS84 decimal GPS coordinate and DD MM SS coordinate format.
- Runs on Android 5.0 or newer.

*) To change the default from Google Maps to a preferred app,
- open <i>Settings</i> on your phone, 
- select <I>Apps</I>, 
- find <i>Maps</i> and select <i>Clear defaults</i>. 
- Head back to <i>My Position</I> and call <I>Show Map</I>. 
- Now you’ll be asked which application to use.  
Pick the one you prefer,
- select <i>This time</I> to test it.  
Next time you may select <i>Always</I> to make sure your preferred app opens by default.

**) To achieve OSM Reverse Geocoding by a more reliable service and for protection against abuse it optionally takes your provided email address  
(see http://wiki.openstreetmap.org/wiki/Nominatim#Reverse_Geocoding).

 
### Permissions
- Location (GPS and Network)
- Internet access (for Reverse Geocoding by OpenStreetMap)

### License
Licensed under GNU GPLv2 or later. (See https://www.gnu.org/licenses/gpl-3.0)
 
This app is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

### History:

My Position copyright by wistein, Bonn, Germany, 2017-2020,<br>
https://github.com/wistein/MyPosition3<BR>

<B>My Position, version 1.3.8,</B> 2021-05-30<br>
 - Use Code library mavenCentral instead of deprecated jCenter.
 - Compiled with Android Studio 4.2.1 and Gradle 6.7.1.
 - Compiled for SdkVersion 30
<br>
<B>My Position, version 1.3.7,</B> 2020-06-27<br>
 - Location handling corrected for coarse location.
 - Compiled with Android Studio 4.0 and Gradle 6.1.1.
 - Migrated to AndroidX libraries
<br>

<B>My Position, version 1.3.6,</B> 2020-02-27<br>
 - GPS handling as background service.
 - Fix values for min. distance and polling time 
 - Help incorporated.
 - Docs revised.
 - Compiled with Android Studio 3.6 and Gradle 5.6.4.
 - Code shrinked.
<br>

<B>My Position, version 1.3.5,</B> 2019-02-03<br>
 - Compiled with Android Studio 3.3 and Gradle 4.10.1.
 - Removed background service for GPS first fix function.
 - Instead direct GPS position request.
 - Some code cleaning.
<br>

<B>My Position, version 1.3.4,</B> 2018-09-19<br>
 - Added option to select a time interval to query the position.
 - GPS update function as background service.
 - Compiled for Android 8.1 (Oreo)
<br>

<B>My Position, version 1.3.3,</B> 2017-10-31<br>
 - Code adapted and compiled under Android Studio 3.0.
<br>

<B>My Position, version 1.3.2</B>, 2017-09-26<br>
  - Code adapted and compiled for Android 7.1.
  - Write system log only in debug version.
  - Showing height and coordinates without irrelevant decimal places.
<br>

<B>My Position version 1.3.1</B>, 2017-09-12<br> 
  - Icons in settings menu
  - Write system log only in debug version
  - Height data is corrected according to World Geodetic System WGS84 and Earth Gravitational Model EGM96.
  - Your Position may be shown either local by a suitable mapping app or online by OpenStreetMap.
  - Optionally you may set a toast message about height values with height above sea-level, GPS-height and height correction.
  - Settings allow to switch between portrait and landscape mode. 
  - Bug fix: Changed app settings are valid without restarting the app.
 
<B>My Position version 1.3.0</B>, 2017-04-11<br> 
  - Code corrections and enhancements.
  - Works without GApps.
  - Interrupts GPS usage when app pauses or ends GPS usage when app terminates.
  - Reverse geocoding for address info by Nominatim service of OpenStreetMap.
  - Optional email address as parameter for polling the reverse geocoding service as demanded by OpenStreetMap (for reliable service).
  - Localization for German and English (default) of text and number formats.
  - Country-specific representation of address info (at, ch, de, fr, it, rest of the world).
  - App-Icon by wistein.
  - Based on the following projects:
  
<B>myLocation version 1.2</B> <BR>
Changes and enhancements by GDR!<br>
https://github.com/gjedeer/mylocation/<br>
License: GNU GPLv2

<B>myLocation version 1.2</B><br>
Copyright 2012 by Mohammad Hafiz Ismail<br>
http://code.google.com/p/mylocation/<br>
License: GNU GPLv2
 
