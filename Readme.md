## MyPosition3
 
### My Position (Mein Standort), version 1.3.1
 
Share your location, easily.
 
My Position determines the current location with address data and GPS coordinates. The application also simplifies the task of sharing your location data with your contacts.
 
Additionally, this application also includes a tool to help converting between WGS84 decimal GPS coordinate and DD MM SS coordinate format.

It uses Reverse Geocoding by OpenStreetMap (OSM) query for showing address info, therefore uses optionally your provided email address as recommended by OSM.
 
It shows a country-specific representation of address info (at, ch, de, fr, it, rest of the world), introduces localized strings (German, English) and considers localized number formats in calculations.
 
Licensed under GNU GPLv2 or later. (See https://www.gnu.org/licenses/gpl-3.0)
 
This app is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 
### History:
 
My Position version 1.3.1, 
copyright by wistein, 2017-08-19 (stein.wm@web.de),
https://github.com/wistein/MyPosition3

  - Height data is corrected according to World Geodetic System WGS84 and Earth Gravitational Model EGM96,
  - Settings allow to set a toast message about height values with height above sea-level, GPS-height and height correction,
  - Bug fix: Changed app settings are valid without restarting the app.
 
My Position version 1.3.0, 
copyright by wistein, 2017-04-11,  

  - Reverse geocoding for address info by Nominatim service of OpenStreetMap,
  - works without GApps,
  - interrupts GPS usage when app pauses or ends GPS usage when app terminates,
  - optional email address as parameter for polling the reverse geocoding service as demanded by OpenStreetMap (for more reliable service),
  - localization for German and English (default) of text and number formats
  
Intermediate Changes and enhancements by GDR
http://f.gdr.name/mylocation.png
License: GNU GPLv2

Based on myLocation version 1.2,
Copyright 2012 by Mohammad Hafiz Ismail
http://code.google.com/p/mylocation/
License: GNU GPLv2
 
App-Icon: wistein
