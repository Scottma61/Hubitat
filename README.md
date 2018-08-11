# Hubitat
MyDrivers
Just my collection of Hubitat drivers that I have collected from others and altered for my own use.
The vast majority of this was not my work.  I just started with someone else's great beginnings and made minor alterations.


 Weather-Display With External Forecast Driver
 
 This driver is intended to pull data from data files on a web server created by Weather-Display software
 (http://www.weather-display.com).  It will also supplement forecast data from  your choice of
 WeatherUnderground (WU)(http://www.wunderground.com) or APIXU.com (XU), but not both simultaneouly. 
 You will need your API keys for each of those APIs to use the forecast from those sites, but it will work
 without either too.
 
 The driver uses the Weather-Display data as the primary dataset.  There are a few options you can select
 from like using your forecast source for illuminance/solar radiation/lux if you do not have those sensors.
 You can also select to use a base set of condition icons from the forecast source, or an 'alternative'
 (fancier) set.  The base set will be from WeatherUnderground if you choose eith er 'None' or 'WeatherUnderground'
 as your forecast source, or from APIXU.com if you choose APIXU as your forecast source.  You may choose the
 fancier 'Alternative' icon set if you have a forecast source other than 'None'.

 To use either driver: Install a virtual device and assign the driver to it  
 
 Many people contributed to the creation of this driver.  Significant contributors include
 @Cobra who adapted it from @mattw01's work and I thank them for that!  A large 'Thank you' 
 to @bangali for his APIXU.COM base code that much of this was adapted from. Also from @bangali
 is the Sunrise-Sunset.org code used to calculate illuminance/lux.  I learned a lot
 from his work and incorporated a lot of that here.  @bangali also contributed the icon work from
 https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy
 of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
 With all of that collaboration I have heavily modified/created new code myself @Matthew (Scottma61)
 with lots of help from the Hubitat community.  This driver is free to use.  I do not accept donations.
 Please feel free to contribute to those mentioned here if you like this work, as it would not have
 possible without them.
 
 *** PLEASE NOTE: You should download and store these 'Alternative' icons on your own server and
 change the reference to that location in the driver. There is no assurance that those icon files will
 remain in my github repository.    ***
