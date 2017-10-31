## MyPosition3
 
### My Position (Mein Standort), version 1.3.3
 
Share your location, easily.
 
My Position determines the current location with address data and GPS coordinates including height above sea-level. 
The application simplifies the task of sharing your location data with your contacts.
 
Your location may also be shown on a map either local (if a suitable app is installed and the default setting of Google Maps is changed*) or by browser on OpenStreetMap.

It uses Reverse Geocoding by OpenStreetMap query for showing address info. To achieve this by a more reliable service and for protection against abuse it optionally takes your provided email address (see http://wiki.openstreetmap.org/wiki/Nominatim#Reverse_Geocoding).
 
It shows a country-specific representation of address info (at, ch, de, fr, it, rest of the world), introduces localized strings and number formats (German, English).
 
Additionally, this application also includes a tool to help converting between WGS84 decimal GPS coordinate and DD MM SS coordinate format.

Licensed under GNU GPLv2 or later. (See https://www.gnu.org/licenses/gpl-3.0)
 
This app is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

*) To change the default from Google Maps to a preferred app, 
- open the settings on your phone, 
- tap Apps, 
- find Maps and 
- select Clear defaults. 

Head back to My Position and call Show Map. 
Now you’ll be asked which application to use.
Pick the one you prefer, and tap `This time´ to test it. 
Next time you may select ‘Always’ to make sure your preferred app opens by default.

### History:
 
<B>My Position, version 1.3.3</B>,<br>
copyright by wistein, 2017-10-31,<br>
https://github.com/wistein/MyPosition3
 - Code adapted and compiled under Android Studio 3.0.
<br>

<B>My Position, version 1.3.2</B>,<br>
copyright by wistein, 2017-09-26,

  - Code adapted and compiled for Android 7.1.
  - Write system log only in debug version.
  - Showing height and coordinates without irrelevant decimal places.

<B>My Position version 1.3.1</B>,<br> 
copyright by wistein, 2017-09-12,

  - Write system log only in debug version
  - Height data is corrected according to World Geodetic System WGS84 and Earth Gravitational Model EGM96.
  - Your Position may be shown either local by a suitable mapping app or online by OpenStreetMap.
  - Optionally you may set a toast message about height values with height above sea-level, GPS-height and height correction.
  - Settings allow to switch between portrait and landscape mode. 
  - Bug fix: Changed app settings are valid without restarting the app.
 
<B>My Position version 1.3.0</B>,<br> 
copyright by wistein, 2017-04-11  

  - Code corrections and enhancements.
  - Works without GApps.
  - Interrupts GPS usage when app pauses or ends GPS usage when app terminates.
  - Reverse geocoding for address info by Nominatim service of OpenStreetMap.
  - Optional email address as parameter for polling the reverse geocoding service as demanded by OpenStreetMap (for reliable service).
  - Localization for German and English (default) of text and number formats.
  - Country-specific representation of address info (at, ch, de, fr, it, rest of the world).
  - App-Icon by wistein.
  
Changes and enhancements by GDR<br>
http://f.gdr.name/mylocation.png<br>
License: GNU GPLv2

<B>Based on myLocation version 1.2</B>,<br>
Copyright 2012 by Mohammad Hafiz Ismail<br>
http://code.google.com/p/mylocation/<br>
License: GNU GPLv2
 
