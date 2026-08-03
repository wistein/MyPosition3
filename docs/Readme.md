# MyPosition3
 
## My Position (Mein Standort)
 
Share your location, easily.

The app simplifies the task of sharing your location data with your contacts.
I have been developing it for myself and published it, so it can be freely used by anyone who likes to.
The software is privacy conscious as it does not make use of Google Play Services or other tracking functions.

### New Google restrictions
Unfortunately, Google is enforcing its monopoly position as they decided to restrict the use of free Android software.
Starting September 2026, a silent update, nonconsensually pushed by Google, will block every Android app whose developers have not registered with Google, signed their contract, paid up a fee, and handed over their government ID.
I am NOT willing to accept neither these registration conditions nor the procedure.
For more background on this and how to overcome the restriction you may visit https://keepandroidopen.org/.

### Features
- My Position determines the current location with address data and GPS coordinates including height above sea-level. 
- Your location may be shown on a map either local (if a suitable app is installed and the default setting of Google Maps is changed*) or by browser on OpenStreetMap.
- It uses Reverse Geocoding by OpenStreetMap (OSM) query for showing address info. **)
- It shows a country-specific representation of address info (at, ch, de, fr, it, rest of the world), introduces localized strings and number formats (German, English).
- Additionally, the app also includes a tool to help converting between WGS84 decimal and DD MM SS GPS coordinates format and calculates the distance between 2 coordinates.
- Runs on Android 7.1 or newer.

*) To change the default from Google Maps to a preferred app,
- open <i>Settings</i> on your phone, 
- select <I>Apps</I>, 
- find <i>Maps</i> and select <i>Clear defaults</i>. 
- Head back to <i>My Position</i> and call <i>Show Map</i>. 
- Now you’ll be asked which application to use. 
- Pick the one you prefer,
- select <i>This time</i> to test it.

Next time you may select <i>Always</i> to make sure your preferred app opens by default.

**) To achieve OSM Reverse Geocoding by a more reliable service and for protection against its abuse it is recommended to provide your email address (see http://wiki.openstreetmap.org/wiki/Nominatim#Reverse_Geocoding).
 
### Permissions
- Location (GPS and Network)
- Internet access (for Reverse Geocoding by OpenStreetMap)

### License
Licensed under GNU GPLv2 or later (see https://www.gnu.org/licenses/gpl-3.0).
 
This app is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

Since versions 1.3.0 to current version, copyright by wistein, 2017-2026,<br>
https://github.com/wistein/MyPosition3<BR>

### History:
<B>My Position, version 1.5.1,</B> 2026-08-03<br>
- All remaining Java files converted to Kotlin
- Code improved<br>
<br>

<B>My Position, version 1.5.0,</B> 2026-05-24<br>
- Code improved<br>
- Bugfixes<br>
<br>

<B>My Position, version 1.4.9,</B> 2026-04-01<br>
- Code adapted to new demands of Nominatim service from OpenStreetMap to get location detail data<br>
- App menu improved<br>
- App only in dark mode<br>
- After 1. GPS fix the reverse geocode locality data is shown without extra request<br>
<br>

<B>My Position, version 1.4.8,</B> 2026-01-23<br>
- Error messages in red<br>
- Some text corrected<br>
<br>

<B>My Position, version 1.4.7,</B> 2025-12-28<br>
- Some code improvements<br>
<br>

<B>My Position, version 1.4.6,</B> 2025-12-18<br>
- Code adapted for Android versions >= 16<br>
- Background location service removed<br>
- Info about last FixTime removed<br>
- Some code improvements<br>
<br>

<B>My Position, version 1.4.5,</B> 2025-09-26<br>
- Code adapted for Android versions >= 15<br>
- App specific and permanent debug support for Emulator in Android Studio<br>
- Some code improvements<br>
- Docs updated<br>
- Compiled for SdkVersion 36<br>
<br>

<B>My Position, version 1.4.3,</B> 2025-03-27<br>
- Background location added<br>
- Permissions handling revised and adapted<br>
- Some code cleaning<br>
- Deprecated functions replaced<br>
- Docs updated<br>
<br>

<B>My Position, version 1.4.2,</B> 2024-12-28<br>
- Some code cleaning<br>
- Deprecated code replaced<br>
- Docs updated<br>
<br>

<B>My Position, version 1.4.1,</B> 2024-12-19<br>
- Minor layout refinements<br>
- Most Java code transposed to Kotlin<br>
- Code refinements<br>
- Docs updated<br>
<br>

- <B>My Position, version 1.4.0,</B> 2024-09-28<br>
- Faster reaction for getting the current position<br>
- Closing the app also frees its memory space<br>
- Most Java code transposed to Kotlin<br>
- Deprecated function replaced<br>
- Code refinements<br>
- Docs updated<br>
- Compiled for SdkVersion 34<br>
<br>

<B>My Position, version 1.3.9,</B> 2023-09-25<br>
- Added calculation for distance between 2 decimal coordinates<br>
- Buttons with icons in calculations view<br>
- ScrollView to show whole calculations view<br>
- Switch off keyboard after distance calculation to show otherwise masked result<br>
- Switch on keyboard when tapped a field<br>
- Code refinements<br>
- Compiled for SdkVersion 33<br>
<br>

<B>My Position, version 1.3.8a,</B> 2021-07-13<br>
 - Compiled for SdkVersion 29: Ok in Android 11<br>
<B>My Position, version 1.3.8,</B> 2021-05-30<br>
 - Use Code library mavenCentral instead of deprecated jCenter<br>
 - Compiled for SdkVersion 30, Problem: Could not be installed in Android 11<br>
<br>

<B>My Position, version 1.3.7,</B> 2020-10-07<br>
 - Permission handling for Access Background Location<br>
 - Migrate Code to AndroidX<br>
<br>

<B>My Position, version 1.3.6,</B> 2020-02-27<br>
 - GPS handling as background service<br>
 - Fix values for min. distance and polling time<br> 
 - Help incorporated<br>
 - Docs revised<br>
 - Code shrunk<br>
<br>

<B>My Position, version 1.3.5,</B> 2019-02-03<br>
 - Removed background service for GPS first fix function<br>
 - Instead direct GPS position request<br>
 - Some code cleaning<br>
<br>

<B>My Position, version 1.3.4,</B> 2018-09-19<br>
 - Added option to select a time interval to query the position<br>
 - GPS update function as background service<br>
 - Compiled for Android 8.1 (Oreo)<br>
<br>

<B>My Position, version 1.3.3,</B> 2017-10-31<br>
 - Code adapted and compiled under Android Studio 3.0<br>
<br>

<B>My Position, version 1.3.2</B>, 2017-09-26<br>
  - Code adapted and compiled for Android 7.1<br>
  - Write system log only in debug version<br>
  - Showing height and coordinates without irrelevant decimal places<br>
<br>

<B>My Position version 1.3.1</B>, 2017-09-12<br> 
  - Icons in settings menu<br>
  - Height data is corrected according to World Geodetic System WGS84 and Earth Gravitational Model EGM96<br>
  - Your Position may be shown either local by a suitable mapping app or online by OpenStreetMap<br>
  - New Option: Set a toast message about height values with height above sea-level, GPS-height and height correction<br>
  - New Option: Switch between portrait and landscape mode.<br>
  - Some improvements to display details<br>
  - Press back twice to exit<br>
  - Bug fixes: Changed app settings are now valid without restarting the app<br>
<br>
 
<B>My Position version 1.3.0</B>, 2017-04-11<br> 
Copyright 2017-2026 by wistein,<br>
  - License: GNU GPLv2<br>
  - Based on the code of myLocation 1.2 with the following changes:<br>
  - Code corrections and enhancements<br>
  - Works without Google Apps GApps<br>
  - Interrupts GPS usage when app pauses or ends GPS usage when app terminates<br>
  - Reverse geocoding for address info by Nominatim service of OpenStreetMap<br>
  - Optional email address as parameter for polling the reverse geocoding service as demanded by OpenStreetMap (for reliable service)<br>
  - System language-specific representation of address info,<br>
  - Localization of text (de German, others English) and<br>
  - Localization of number formats ("." English, "," others<br>
  - App-Icon by wistein<br>
<br>

Based on <br>
<B>myLocation version 1.2</B> <BR>
Copyright 2012 by Mohammad Hafiz Ismail<br>
http://code.google.com/p/mylocation/<br>
Changes and enhancements by GDR!<br>
https://github.com/gjedeer/mylocation/<br>
License: GNU GPLv2.<br>>
<br>
