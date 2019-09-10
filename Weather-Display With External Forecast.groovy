import groovy.transform.Field

metadata {
    definition (name: "WD With External Forecast Driver-Attribute Test", namespace: "Matthew", author: "Scottma61") {
        capability "Actuator"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
 		capability "Pressure Measurement"
 		capability "Ultraviolet Index"
	
		attributesMap.each
		{
			k, v -> if (("${k}Publish") == true && v.typeof) attribute "${k}", "${v.typeof}"
		}

    // some attributes are 'doubled' due to some dashboard requirements (SmartTiles & SharpTools)
	//  the additional doubled attributes are added here:
		attribute "weatherIcons", "string"
        attribute "chanceOfRain", "string"

		attribute "local_sunrise",   "string"	// localSunrisePublish related
		attribute "local_sunset",    "string"	// localSunrisePublish   |
		attribute "localSunrise",    "string"	// localSunrisePublish   |
		attribute "localSunset",     "string"	// localSunrisePublish   |

		attribute "lat",             "number"	// latPublish related
		attribute "lon",             "number"	// latPublish   |

		attribute "betwixt",       "string"
        command "pollData"
         
    }
    def settingDescr = settingEnable ? "<br><i>Hide many of the Preferences to reduce the clutter, if needed, by turning OFF this toggle.</i><br>" : "<br><i>Many Preferences are available to you, if needed, by turning ON this toggle.</i><br>"
    
    preferences() {
		section("Query Inputs"){
            input "extSource", "enum", title: "Select External Source", required:true, defaultValue: 1, options: [1:"None", 2:"Apixu", 3:"DarkSky"]
            input "pollIntervalStation", "enum", title: "Station Poll Interval", required: true, defaultValue: "3 Hours", options: ["Manual Poll Only", "1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "stationLatHemi", "enum", title: "Station Northern or Southern hemisphere?", required: true, defaultValue: "North", options: ["North", "South"]
            input "stationLongHemi", "enum", title: "Station East or West of GMT (London, UK)?", required: true, defaultValue: "West", options: ["West", "East"]
            input "pollLocationStation", "text", required: true, title: "Station Data File Location:", defaultValue: "http://"
			input "apiKey", "text", required: true, defaultValue: "Type API Key Here", title: "API Key"
			input "pollIntervalForecast", "enum", title: "External Source Poll Interval", required: true, defaultValue: "3 Hours", options: ["Manual Poll Only", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
			input "pollLocationForecast", "text", required: true, title: "Forecast location: ZIP Code (APIXU) or Latitude,longitude (DarkSky)"
			input "sourceImg", "bool", required: true, defaultValue: false, title: "Icons from: On = Standard - Off = Alternative"
			input "iconLocation", "text", required: true, defaultValue: "https://raw.githubusercontent.com/Scottma61/WeatherIcons/master/", title: "Alternative Icon Location:"
            input "iconType", "bool", title: "Condition Icon: On = Current - Off = Forecast", required: true, defaultValue: false
	    	input "tempFormat", "enum", required: true, defaultValue: "Fahrenheit (°F)", title: "Display Unit - Temperature: Fahrenheit (°F) or Celsius (°C)",  options: ["Fahrenheit (°F)", "Celsius (°C)"]
            input "datetimeFormat", "enum", required: true, defaultValue: "m/d/yyyy 12 hour (am|pm)", title: "Display Unit - Date-Time Format",  options: [1:"m/d/yyyy 12 hour (am|pm)", 2:"m/d/yyyy 24 hour", 3:"mm/dd/yyyy 12 hour (am|pm)", 4:"mm/dd/yyyy 24 hour", 5:"d/m/yyyy 12 hour (am|pm)", 6:"d/m/yyyy 24 hour", 7:"dd/mm/yyyy 12 hour (am|pm)", 8:"dd/mm/yyyy 24 hour", 9:"yyyy/mm/dd 24 hour"]
            input "distanceFormat", "enum", required: true, defaultValue: "Miles (mph)", title: "Display Unit - Distance/Speed: Miles or Kilometres",  options: ["Miles (mph)", "Kilometers (kph)"]
            input "pressureFormat", "enum", required: true, defaultValue: "Inches", title: "Display Unit - Pressure: Inches or Millibar",  options: ["Inches", "Millibar"]
            input "rainFormat", "enum", required: true, defaultValue: "Inches", title: "Display Unit - Precipitation: Inches or Millimetres",  options: ["Inches", "Millimetres"]
            input "summaryType", "bool", title: "Full Weather Summary", required: true, defaultValue: false
            input "logSet", "bool", title: "Create extended Logging", required: true, defaultValue: false
            input "sourcefeelsLike", "bool", required: true, title: "Feelslike from Weather-Display?", defaultValue: false
		    input "sourceIllumination", "bool", required: true, title: "Illuminance from Weather-Display?", defaultValue: true
            input "sourceUV", "bool", required: true, title: "UV from Weather-Display?", defaultValue: true
            input "settingEnable", "bool", title: "<b>Display All Preferences</b>", description: "$settingDescr", defaultValue: true
	// build a Selector for each mapped Attribute or group of attributes
	    	attributesMap.each
		    {
	    		keyname, attribute ->
	    		if (settingEnable) input "${keyname}Publish", "bool", title: "${attribute.title}", required: true, defaultValue: "${attribute.default}", description: "<br>${attribute.descr}<br>"
	    	}
        }
    }
}
// <<<<<<<<<< Begin Sunrise-Sunset Poll Routines >>>>>>>>>>
def pollSunRiseSet() {
	if (loc_lat) {
        currDate = new Date().format("yyyy-MM-dd", TimeZone.getDefault())
        log.info("Weather-Display Driver - INFO: Polling Sunrise-Sunset.org")
		def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=$loc_lat&lng=$loc_lon&formatted=0" ]
		if (currDate) {requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=$loc_lat&lng=$loc_lon&formatted=0&date=$currDate" ]}
        LOGINFO("Poll Sunrise-Sunset: $requestParams")
		asynchttpGet("sunRiseSetHandler", requestParams)
	} else {
		log.warn "No Sunrise-Sunset without Lat/Long."
	}
    return
}

def sunRiseSetHandler(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		sunRiseSet = resp.getJson().results
		updateDataValue("sunRiseSet", resp.data)
        LOGINFO("Sunrise-Sunset Data: $sunRiseSet")
        setDateTimeFormats(datetimeFormat)
        updateDataValue("currDate", new Date().format("yyyy-MM-dd", TimeZone.getDefault()))
        updateDataValue("currTime", new Date().format("HH:mm", TimeZone.getDefault()))
		updateDataValue("riseTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format("HH:mm", TimeZone.getDefault()))     
        updateDataValue("noonTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon).format("HH:mm", TimeZone.getDefault()))
		updateDataValue("setTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format("HH:mm", TimeZone.getDefault()))
        updateDataValue("tw_begin", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin).format("HH:mm", TimeZone.getDefault()))
        updateDataValue("tw_end", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end).format("HH:mm", TimeZone.getDefault()))
		updateDataValue("localSunset",new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
		updateDataValue("localSunrise", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
	    if(getDataValue("riseTime") <= getDataValue("currTime") && getDataValue("setTime") >= getDataValue("currTime")) {
            updateDataValue("is_day", "1")
        } else {
            updateDataValue("is_day", "0")
        }
    } else {
		log.warn "Sunrise-Sunset api did not return data"
	}
    return
}
// >>>>>>>>>> End Sunrise-Sunset Poll Routines <<<<<<<<<<

// <<<<<<<<<< Begin Weather-Display Poll Routines >>>>>>>>>>
def pollWD() {
    log.info("Weather-Display Driver - INFO: Polling Weather-Display")    
	def ParamsWD = [ uri: "${pollLocationStation}everything.php" ]
    LOGINFO("Poll Weather-Display: $ParamsWD")
	asynchttpGet("pollWDHandler", ParamsWD)
    return
}

def pollWDHandler(resp, data) {
    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        wd = parseJson(resp.data)
        LOGINFO("Weather-Display Data: $wd")
		doPollWD()		// parse the data returned by ApiXU
	} else {
		log.error "Weather-Display weather api did not return data"
	}
    return
}

def doPollWD() {
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
    setDateTimeFormats(datetimeFormat)
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)    
    updateDataValue("currDate", new Date().format("yyyy-MM-dd", TimeZone.getDefault()))
    updateDataValue("currTime", new Date().format("HH:mm", TimeZone.getDefault()))
    if(getDataValue("riseTime") <= getDataValue("currTime") && getDataValue("setTime") >= getDataValue("currTime")) {
        updateDataValue("is_day", "1")
    } else {
        updateDataValue("is_day", "0")
    }
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<  
// <<<<<<<<<< Begin Setup Station Variables >>>>>>>>>>        
    sotime = new Date().parse("HH:mm dd/MM/yyyy", wd.time.time_date, TimeZone.getDefault())
    updateDataValue("sotime", sotime.toString())
    sutime = new Date().parse("HH:mm dd/MM/yyyy", wd.time.time_date, TimeZone.getDefault())
    updateDataValue("sutime", sutime.toString())
// >>>>>>>>>> End Setup Station Variables <<<<<<<<<<                
// <<<<<<<<<< Begin Process Only If No External Forcast Is Selected  >>>>>>>>>>                
    if(extSource.toInteger() == 1){
        fotime = new Date().parse("HH:mm d/M/yyyy", wd.time.time_date, TimeZone.getDefault())
        futime = new Date().parse("HH:mm d/M/yyyy", wd.time.time_date, TimeZone.getDefault())
        possAlert = null
        updateDataValue("alert", "Weather alerts are not available from this source.")
        updateDataValue("percentPrecip", 'Not available from this source.')
        if(!wd.everything.weather.solar.percentage){
            cloud = 1
        } else {
            if(wd.everything.weather.solar.percentage.toInteger() == 100){    
                updateDataValue("cloud", "1")
            } else {
                updateDataValue("cloud",(100 - wd.everything.weather.solar.percentage.toInteger()).toString())
            }
        }
        c_code = (getDataValue("is_day")=="1" ? '' : 'nt_')
        switch(!wd.everything.forecast.icon.code ? 99 : wd.everything.forecast.icon.code.toInteger()) {
            case 0: c_code += 'sunny'; break;    
            case 1: c_code += 'clear'; break;        
            case 2: c_code += 'partlycloudy'; break;
            case 3: c_code += 'clear'; break;    
            case 4: c_code += 'cloudy'; break;   
            case 5: c_code += 'clear'; break;    		
            case 6: c_code += 'fog'; break;
            case 7: c_code += 'hazy'; break;
            case 8: c_code += 'rain'; break;        
            case 9: c_code += 'clear'; break;    
            case 10: c_code += 'hazy'; break;
            case 11: c_code += 'fog'; break;    
            case 12: c_code += 'rain'; break;        
            case 13: c_code += 'mostlycloudy'; break;    
            case 14: c_code += 'rain'; break;        
            case 15: condition_code += 'rain'; break;        
            case 16: c_code += 'snow'; break;        
            case 17: c_code += 'tstorms'; break;            
            case 18: c_code += 'mostlycloudy'; break;
            case 19: c_code += 'mostlycloudy'; break;
            case 20: c_code += 'rain'; break;
            case 21: c_code += 'rain'; break;    
            case 22: c_code += 'rain'; break;
            case 23: c_code += 'sleet'; break;
            case 24: c_code += 'sleet'; break;
            case 25: c_code += 'snow'; break;
            case 26: c_code += 'snow'; break;
            case 27: c_code += 'snow'; break;
            case 28: c_code += 'sunny'; break;
            case 29: c_code += 'tstorms'; break;
            case 30: c_code += 'tstorms'; break;
            case 31: c_code += 'tstorms'; break;
            case 32: c_code += 'tstorms'; break;
            case 33: c_code += 'breezy'; break;
            case 34: c_code += 'partlycloudy'; break;		
            case 35: c_code += 'rain'; break;
            default: c_code += 'unknown'; break;
        }
        updateDataValue("condition_code", c_code)
        updateDataValue("condition_text", getDSTextName(c_code, 'Empty'))
        updateDataValue("vis", "Visibility is not available from this source.")        
        def (holdlux, bwn) = estimateLux(getDataValue("condition_code"),getDataValue("cloud")) //condition_code, cloud)
        updateDataValue("bwn", bwn)
    }
// >>>>>>>>>> End Process Only If No External Forcast Is Selected  <<<<<<<<<<                        
// <<<<<<<<<< Begin Process Only If Illumination from WD Is Selected  >>>>>>>>>>                                
    if(sourceIllumination == true){
        if (!wd.everything.weather.solar.irradiance.wm2){
//            updateDataValue("lux", "This station does not send lux data.")
            updateDataValue("illuminance", "This station does not send illuminance data.")
            updateDataValue("illuminated", "This station does not send illuminance data.")
        } else {
//            updateDataValue("lux", wd.everything.weather.solar.irradiance.wm2.toInteger().toString())
            updateDataValue("illuminance", wd.everything.weather.solar.irradiance.wm2.toInteger().toString())
            updateDataValue("illuminated", String.format("%,4d", wd.everything.weather.solar.irradiance.wm2.toInteger()).toString())
        }
    }
// >>>>>>>>>> End Process Only If Illumination from WD Is Selected  <<<<<<<<<<
// <<<<<<<<<< Begin Process Only If UV from WD Is Selected  >>>>>>>>>>                
    if(sourceUV==true && UVPublish){
        if(!wd.everything.weather.uv.uvi){
            updateDataValue("uv", "This station does not send ultravoilet index data.")
        } else {
            updateDataValue("uv", wd.everything.weather.uv.uvi)
        }
    }        
// >>>>>>>>>> End Process Only If UV from WD Is Selected  <<<<<<<<<<        
// <<<<<<<<<< Begin Process Standard Weather-Station Variables (Regardless of Forecast Selection)  >>>>>>>>>>    
    updateDataValue("wind_bft_icon", 'wb' + wd.everything.weather.wind.avg_speed.bft.toInteger().toString() + '.png')
    switch(wd.everything.weather.wind.avg_speed.bft.toInteger()){
        case 0: w_string_bft = "Calm"; break;
        case 1: w_string_bft = "Light air"; break;
        case 2: w_string_bft = "Light breeze"; break;
        case 3: w_string_bft = "Gentle breeze"; break;
        case 4: w_string_bft = "Moderate breeze"; break;
        case 5: w_string_bft = "Fresh breeze"; break;
        case 6: w_string_bft = "Strong breeze"; break;
        case 7: w_string_bft = "High wind, moderate gale, near gale"; break;
        case 8: w_string_bft = "Gale, fresh gale"; break;
        case 9: w_string_bft = "Strong/severe gale"; break;
        case 10: w_string_bft = "Storm, whole gale"; break;
        case 11: w_string_bft = "Violent storm"; break;
        case 12: w_string_bft = "Hurricane force"; break;
        default: w_string_bft = "Calm"; break;
    }
    updateDataValue("city", wd.station.name.split(/ /)[0])
    updateDataValue("state", wd.station.name.split(/ /)[1])
    updateDataValue("country", wd.station.name.split(/ /)[2])
    updateDataValue("wslocation", wd.station.name.split(/ /)[0] + ", " + wd.station.name.split(/ /)[1])
    updateDataValue("moonAge", wd.everything.astronomy.moon.moon_age.toBigDecimal().toString())
    if(moonPhasePublish){
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 0 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 4) {mPhase = "New Moon"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 4 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 7) {mPhase = "Waxing Crescent"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 7 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 10) {mPhase = "First Quarter"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 10 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 14) {mPhase = "Waxing Gibbous"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 14 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 18) {mPhase = "Full Moon"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 18 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 22) {mPhase = "Waning Gibbous"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 22 && wd.everything.astronomy.moon.moon_age.toBigDecimal() < 26) {mPhase = "Last Quarter"}
        if (wd.everything.astronomy.moon.moon_age.toBigDecimal() >= 26) {mPhase = "Waxing Gibbous"}
        updateDataValue("moonPhase", mPhase)
    }
    if(solarradiationPublish){
        if(!wd.everything.weather.solar.irradiance.wm2){
            updateDataValue("solarradiation", "This station does not send Solar Radiation data.")
        } else {
            updateDataValue("solarradiation", wd.everything.weather.solar.irradiance.wm2.toInteger().toString())
        }
    }
    if(sourcefeelsLike==true){
        updateDataValue("feelsLike", (isFahrenheit ? wd.everything.weather.apparent_temperature.current.f.toBigDecimal() : wd.everything.weather.apparent_temperature.current.c.toBigDecimal()).toString())
    }
    if(dewpointPublish){updateDataValue("dewpoint", (isFahrenheit ? wd.everything.weather.dew_point.current.f : wd.everything.weather.dew_point.current.c))}
    updateDataValue("humidity", wd.everything.weather.humidity.current.toString())
    updateDataValue("precip_today", isRainMetric ? wd.everything.weather.rainfall.daily.mm.toString() : wd.everything.weather.rainfall.daily.in.toString())
    updateDataValue("pressure", (isPressureMetric ? wd.everything.weather.pressure.current.mb.toString() : wd.everything.weather.pressure.current.inhg.toString()))
    updateDataValue("temperature", (isFahrenheit ? wd.everything.weather.temperature.current.f.toString() : wd.everything.weather.temperature.current.c.toString()))
    updateDataValue("wind", (isDistanceMetric ? Math.round(wd.everything.weather.wind.avg_speed.kmh.toBigDecimal() * 10) / 10 : Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() * 10) / 10).toString())
    updateDataValue("wind_gust", (isDistanceMetric ? (Math.round(wd.everything.weather.wind.gust_speed.kmh.toBigDecimal() * 10) / 10) : (Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal() * 10) / 10)).toString())
    updateDataValue("wind_degree", wd.everything.weather.wind.direction.degrees.toInteger().toString())
    switch(wd.everything.weather.wind.direction.cardinal.toUpperCase()){
        case 'N': w_direction = 'North'; break;
        case 'NNE': w_direction = 'North-Northeast'; break;
        case 'NE': w_direction = 'Northeast'; break;
        case 'ENE': w_direction = 'East-Northeast'; break;
        case 'E': w_direction = 'East'; break;
        case 'ESE': w_direction = 'East-Southeast'; break;
        case 'SE': w_direction = 'Southeast'; break;
        case 'SSE': w_direction = 'South-Southeast'; break;
        case 'S': w_direction = 'South'; break;
        case 'SSW': w_direction = 'South-Southwest'; break;
        case 'SW': w_direction = 'Southwest'; break;
        case 'WSW': w_direction = 'West-Southwest'; break;
        case 'W': w_direction = 'West'; break;
        case 'WNW': w_direction = 'West-Northwest'; break;
        case 'NW': w_direction = 'Northwest'; break;
        case 'NNW': w_direction = 'North-Northwest'; break;
        default: w_direction = 'Unknown'; break;
    }
    updateDataValue("wind_direction", w_direction)
    updateDataValue("wind_string", w_string_bft + " from the " + getDataValue("wind_direction") + (getDataValue("wind").toBigDecimal() < 1.0 ? '': " at " + getDataValue("wind") + (isDistanceMetric ? " KPH" : " MPH")))
// >>>>>>>>>> End Process Standard Weather-Station Variables (Regardless of Forecast Selection)  <<<<<<<<<<
    if(extSource.toInteger() == 1){
        if(sourceImg==false){
            imgName = (getDataValue("iconType")== 'true' ? getImgName(getDataValue("condition_text"), getDataValue("condition_code")) : getImgName(getDataValue("forecast_text"), getDataValue("forecast_code")))
            if(condition_iconPublish){sendEventPublish(name: "condition_icon", value: '<img src=' + imgName + '>')}
            if(condition_iconWithTextPublish){sendEventPublish(name: "condition_iconWithText", value: "<img src=" + imgName + "><br>" + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forcast_text")))}
            if(condition_icon_urlPublish){sendEventPublish(name: "condition_icon_url", value: imgName)}
            updateDataValue("condition_icon_url", imgName)
            if(condition_icon_onlyPublish){sendEventPublish(name: "condition_icon_only", value: imgName.split("/")[-1].replaceFirst("\\?raw=true",""))}
	    } else if(sourceImg==true) {
            if(condition_iconPublish){sendEventPublish(name: "condition_icon", value: '<img src=https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) + '.gif>')}
            if(condition_iconWithTextPublish){sendEventPublish(name: "condition_iconWithText", value: '<img src=https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) + '.gif><br>' + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forecast_text")))}
            if(condition_icon_urlPublish){sendEventPublish(name: "condition_icon_url", value: 'https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')}
            updateDataValue("condition_icon_url", 'https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')
            if(condition_icon_onlyPublish){sendEventPublish(name: "condition_icon_only", value: getDataValue("condition_code") +'.gif')}
	    }
        Summary_forecastTemp = ". "
        Summary_vis = ""
    }
    if(getDataValue("forecastPoll") == "false"){
        if(extSource.toInteger() == 2){
            pollXU()
        } else if(extSource.toInteger() == 3){
            pollDS()
        }
    }
    PostPoll()
}
// >>>>>>>>>> End Weather-Display routines <<<<<<<<<<

// <<<<<<<<< Begin ApiXU routines >>>>>>>>>>>>>>>>
def pollXU() {
    log.info "Weather-Display Driver - INFO: Polling ApiXU.com"
	def ParamsXU = [ uri: "https://api.apixu.com/v1/forecast.json?key=$apiKey&q=$pollLocationForecast&days=3" ]
    LOGINFO("Poll ApiXU: $ParamsXU")
	asynchttpGet("pollXUHandler", ParamsXU)
    return
}

def pollXUHandler(resp, data) {
    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        xu = parseJson(resp.data)
        LOGINFO("ApiXU Data: $xu")
		doPollXU()		// parse the data returned by ApiXU
	} else {
		log.error "ApiXU weather api did not return data"
	}
    return
}

def doPollXU() {
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
    setDateTimeFormats(datetimeFormat)    
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
    updateDataValue("currDate", new Date().format("yyyy-MM-dd", TimeZone.getDefault()))
    updateDataValue("currTime", new Date().format("HH:mm", TimeZone.getDefault()))    
    if(getDataValue("riseTime") <= getDataValue("currTime") && getDataValue("setTime") >= getDataValue("currTime")) {
        updateDataValue("is_day", "1")
    } else {
        updateDataValue("is_day", "0")
    }
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<  
// <<<<<<<<<< Begin Setup ApiXU Forecast Variables >>>>>>>>>>            
	fotime = new Date().parse("yyyy-MM-dd HH:mm", "${xu.current.last_updated}", TimeZone.getDefault())
    updateDataValue("fotime", fotime.toString())
	futime = new Date().parse("yyyy-MM-dd HH:mm", "${xu.location.localtime}", TimeZone.getDefault())
    updateDataValue("futime", futime.toString())
    updateDataValue("alert", "Weather alerts are not available from this source.")
	possAlert = null
	if(sourcefeelsLike==false){	
        updateDataValue("feelsLike", (isFahrenheit ? xu.current.feelslike_f : xu.current.feelslike_c).toString()) // only needed for SmartTiles dashboard
	}
// <<<<<<<<<< Begin Process Only If ApiXU External Forcast Is Selected  >>>>>>>>>>                    
	if(extSource.toInteger() == 2) {
        updateDataValue("condition_code", getWUCodeName(xu.current.condition.code))
        updateDataValue("condition_text", getDSTextName(xu.current.condition.text, getWUCodeName(xu.current.condition.code)))
        updateDataValue("forecast_code", getWUCodeName(xu.forecast.forecastday[0].day.condition.code))
        updateDataValue("forecast_text", getDSTextName(xu.forecast.forecastday[0].day.condition.code, getDataValue("forecast_code")))
        updateDataValue("cloud", (xu.current.cloud.toInteger() == 0 ? "1" : xu.current.cloud.toString()))
        updateDataValue("vis", (isDistanceMetric ? xu.current.vis_km.toBigDecimal() : xu.current.vis_miles.toBigDecimal()).toString())
        if(sourceUV==false && UVPublish){updateDataValue("uv", xu.forecast.forecastday[0].day.uv.toString())}
        updateDataValue("percentPrecip", "Not Available")
        updateDataValue("forecastHigh", (isFahrenheit ? xu.forecast.forecastday[0].day.maxtemp_f : xu.forecast.forecastday[0].day.maxtemp_c).toString())
        updateDataValue("forecastLow", (isFahrenheit ? xu.forecast.forecastday[0].day.mintemp_f : xu.forecast.forecastday[0].day.mintemp_c).toString())
        if(precipExtendedPublish){
            updateDataValue("rainTomorrow", (isRainMetric ? xu.forecast.forecastday[1].day.totalprecip_mm : xu.forecast.forecastday[1].day.totalprecip_in).toString())
            updateDataValue("rainDayAfterTomorrow", (isRainMetric ? xu.forecast.forecastday[2].day.totalprecip_mm : xu.forecast.forecastday[2].day.totalprecip_in).toString())
        }
// <<<<<<<<<< Begin Process Only If Illumination from WD Is NOT Selected  >>>>>>>>>>                                
        if(sourceIllumination==false) {
			def (lux, bwn) = estimateLux(getDataValue("condition_code"), getDataValue("cloud"))
    	    sendEventPublish(name: "illuminance", value: lux)
            sendEventPublish(name: "illuminated", value: String.format("%,4d lux", lux))            
    	    sendEventPublish(name: "lux", value: String.format("%,4d", lux), unit: lux)
            sendEventPublish(name: "betwixt", value: bwn)
		}
// >>>>>>>>>> End Process Only If Illumination from WD Is NOT Selected  <<<<<<<<<<                  
	}
    if(sourceImg == false){ //Alternative icons
        imgName = getImgName((getDataValue("iconType")== 'true' ? xu.current.condition.code.toInteger() : xu.forecast.forecastday[0].day.condition.code.toInteger()))
        if(condition_iconPublish){sendEventPublish(name: "condition_icon", value: '<img src=' + imgName + '>')}
        if(condition_iconWithTextPublish){sendEventPublish(name: "condition_iconWithText", value: "<img src=" + imgName + "><br>" + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forecast_text")))}
        if(condition_icon_urlPublish){sendEventPublish(name: "condition_icon_url", value: imgName)}
    	updateDataValue("condition_icon_url", imgName)
        if(condition_icon_onlyPublish){sendEventPublish(name: "condition_icon_only", value: imgName.split("/")[-1].replaceFirst("\\?raw=true",""))}
    } else if(sourceImg==true) {
        if(condition_iconPublish){sendEventPublish(name: "condition_icon", value: (getDataValue("iconType") ? '<img src=http:' + xu.current.condition.icon + '>' : '<img src=http:' + xu.forecast.forecastday[0].day.condition.icon + '>'))}
        if(condition_iconWithTextPublish){sendEventPublish(name: "condition_iconWithText", value: (getDataValue("iconType") ? '<img src=http:' + xu.current.condition.icon + '>' : '<img src=http:' + xu.forecast.forecastday[0].day.condition.icon + '>') + '<br>' + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forecast_text")))}
        if(condition_icon_urlPublish){sendEventPublish(name: "condition_icon_url", value: (getDataValue("iconType") ? 'https:' + xu.current.condition.icon : 'https:' + xu.forecast.forecastday[0].day.condition.icon))}
        updateDataValue("condition_icon_url", (getDataValue("iconType") ? 'https:' + xu.current.condition.icon : 'https:' + xu.forecast.forecastday[0].day.condition.icon))
        if(condition_icon_onlyPublish){sendEventPublish(name: "condition_icon_only", value: (getDataValue("iconType") ? xu.current.condition.icon.split("/")[-1] : xu.forecast.forecastday[0].day.condition.icon.split("/")[-1]))}
    }
    if(getDataValue("forecastPoll") == "false"){
        updateDataValue("forecastPoll", "true")
    }
    PostPoll()
// >>>>>>>>>> End Process Only If ApiXU External Forecast Is Selected  <<<<<<<<<<    		
}
// >>>>>>>>>> End ApiXU Poll Routines <<<<<<<<<<

// <<<<<<<<<< Begin DarkSky Poll Routines >>>>>>>>>>
def pollDS() {
    if(pollLocationForecast.contains(',')==false) {
        log.error "<<< Dark Sky requires 'latitude,longtitude' for forecast location.  Please correct this. >>>"
    } else {
        pollLocationForecastClean = pollLocationForecast.replace(" ", "")
    }
	def ParamsDS = [ uri: "https://api.darksky.net/forecast/${apiKey}/${pollLocationForecastClean}?units=us&exclude=minutely,hourly,flags" ]
    LOGINFO("Poll DarkSky: $ParamsDS")
	asynchttpGet("pollDSHandler", ParamsDS)
    return
}

def pollDSHandler(resp, data) {
    log.info "Weather-Display Driver - INFO: Polling DarkSky.net"
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        ds = parseJson(resp.data)
        LOGINFO("DarkSky Data: $ds")
		doPollDS()		// parse the data returned by DarkSky
	} else {
		log.error "DarkSky weather api did not return data"
	}
}

def doPollDS() {
    // <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
    setDateTimeFormats(datetimeFormat)    
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
    updateDataValue("currDate", new Date().format("yyyy-MM-dd", TimeZone.getDefault()))
    updateDataValue("currTime", new Date().format("HH:mm", TimeZone.getDefault()))    
    if(getDataValue("riseTime") <= getDataValue("currTime") && getDataValue("setTime") >= getDataValue("currTime")) {
        updateDataValue("is_day", "1")
    } else {
        updateDataValue("is_day", "0")
    }
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<  
    fotime = new Date(ds.currently.time * 1000L)
    updateDataValue("fotime", fotime.toString())
	futime = new Date(ds.currently.time * 1000L)
    updateDataValue("futime", futime.toString())
    if (!ds.currently.cloudCover) {
        cloudCover = 1
    } else {
        cloudCover = (ds.currently.cloudCover == 0) ? 1 : ds.currently.cloudCover * 100
    }
    updateDataValue("cloud", cloudCover.toInteger().toString())
    possAlert = ds.alerts
    if (!ds.alerts){
        updateDataValue("alert", "No current weather alerts for this area.")
    } else {
        updateDataValue("alert", ds.alerts.title.toString().replaceAll("[{}\\[\\]]", "").split(/,/)[0] + '.')
    }
    if(sourcefeelsLike==false){
        updateDataValue("feelsLike", (isFahrenheit ? (Math.round(ds.currently.apparentTemperature.toBigDecimal() * 10) / 10).toString() : (Math.round((ds.currently.apparentTemperature.toBigDecimal() * (9/5) + 32) * 10) / 10)).toString())
    }    
    if(sourceUV==false && UVPublish){    
        updateDataValue("uv", value: ds.currently.uvIndex)
    }
    updateDataValue("vis", (isDistanceMetric ? (Math.round(ds.currently.visibility.toBigDecimal() * 1.60934 * 10) / 10).toString() : (Math.round(ds.currently.visibility.toBigDecimal() * 10) / 10).toString()))
    updateDataValue("percentPrecip", (ds.daily.data[0].precipProbability * 100).toInteger().toString())
    switch(ds.daily.data[0].icon){
        case "clear-day": f_code =  "sunny"; break;
        case "clear-night": f_code =  "nt_clear"; break;
        case "rain": f_code = (getDataValue("is_day")=="1" ? "rain" : "nt_rain"); break;
        case "wind": f_code = (getDataValue("is_day")=="1" ? "breezy" : "nt_breezy"); break;
        case "snow": f_code = (getDataValue("is_day")=="1" ? "snow" : "nt_snow"); break;
        case "sleet": f_code = (getDataValue("is_day")=="1" ? "sleet" : "nt_sleet"); break;
        case "fog": f_code = (getDataValue("is_day")=="1" ? "fog" : "nt_fog"); break;
        case "cloudy": f_code = (getDataValue("is_day")=="1" ? "cloudy" : "nt_cloudy"); break;
        case "partly-cloudy-day": f_code = "partlycloudy"; break;
        case "partly-cloudy-night": f_code = "nt_partlycloudy"; break;
        default: f_code = "unknown"; break;
    }
    updateDataValue("forecast_code", f_code)
    updateDataValue("forecast_text", getDSTextName(ds.currently.summary, f_code))
    switch(ds.currently.icon) {
        case "clear-day": c_code = "sunny"; break;
        case "clear-night": c_code = "nt_clear"; break;
        case "rain": c_code = (getDataValue("is_day")=="1" ? "rain" : "nt_rain"); break;
        case "wind": c_code = (getDataValue("is_day")=="1" ? "breezy" : "nt_breezy"); break;
        case "snow": c_code = (getDataValue("is_day")=="1" ? "snow" : "nt_snow"); break;
        case "sleet": c_code = (getDataValue("is_day")=="1" ? "sleet" : "nt_sleet"); break;
        case "fog": c_code = (getDataValue("is_day")=="1" ? "fog" : "nt_fog"); break;
        case "cloudy": c_code = (getDataValue("is_day")=="1" ? "cloudy" : "nt_cloudy"); break;
        case "partly-cloudy-day": c_code = "partlycloudy"; break;
        case "partly-cloudy-night": c_code = "nt_partlycloudy"; break;
        default: c_code = "unknown"; break;
    }
    updateDataValue("condition_code", c_code)
    updateDataValue("condition_text", getDSTextName(ds.currently.summary, c_code))
    updateDataValue("forecastHigh", (isFahrenheit ?(Math.round(ds.daily.data[0].temperatureHigh.toBigDecimal() * 10) / 10): Math.round((ds.daily.data[0].temperatureHigh.toBigDecimal() * (9/5) + 32) * 10) / 10).toString())
    updateDataValue("forecastLow", (isFahrenheit ? Math.round(ds.daily.data[0].temperatureLow.toBigDecimal() * 10) / 10 : Math.round((ds.daily.data[0].temperatureLow.toBigDecimal() * (9/5) + 32) * 10) / 10).toString())
    if(precipExtendedPublish){
        updateDataValue("rainTomorrow", Math.round(ds.daily.data[1].precipProbability.toBigDecimal() * 100).toString())
        updateDataValue("rainDayAfterTomorrow", Math.round(ds.daily.data[2].precipProbability.toBigDecimal() * 100).toString())
    }
    if(sourceIllumination==false) {
        def (lux, bwn) = estimateLux(getDataValue("condition_code"), getDataValue("cloud"))
//        updateDataValue("lux", lux.toString())
        updateDataValue("bwn", bwn)
        updateDataValue("illuminance", lux.toString())
        updateDataValue("illuminated", String.format("%,4d", lux).toString())
    }
    if(sourceImg==false){
        imgName = (getDataValue("iconType")== 'true' ? getDSImgName(getDSTextName(ds.currently.summary, getDataValue("condition_code")), getDataValue("condition_code")) : getDSImgName(getDataValue("forecast_text"), getDataValue("forecast_code")))
        if(condition_iconPublish){sendEventPublish(name: "condition_icon", value: '<img src=' + imgName + '>')}
        if(condition_iconWithTextPublish){sendEventPublish(name: "condition_iconWithText", value: "<img src=" + imgName + "><br>" + (getDataValue("iconType")== 'true' ? getDSTextName(ds.currently.summary, getDataValue("condition_code")) : getDSTextName(ds.currently.summary, getDataValue("forecast_code"))))}
        if(condition_icon_urlPublish){sendEventPublish(name: "condition_icon_url", value: imgName)}
        updateDataValue("condition_icon_url", imgName)
        if(condition_icon_onlyPublish){sendEvent(name: "condition_icon_only", value: imgName.split("/")[-1].replaceFirst("\\?raw=true",""))}
    } else if(sourceImg==true) {
        if(condition_iconPublish){sendEventPublish(name: "condition_icon", value: '<img src=https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) + '.gif>')}
        if(condition_iconWithTextPublish){sendEventPublish(name: "condition_iconWithText", value: '<img src=https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) + '.gif><br>' + (getDataValue("iconType")== 'true' ? getDSTextName(ds.currently.summary, getDataValue("condition_code")) : getDSTextName(ds.currently.summary, getDataValue("forecast_code"))))}
        if(condition_icon_urlPublish){sendEventPublish(name: "condition_icon_url", value: 'https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')}
        updateDataValue("condition_icon_url", 'https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')
        if(condition_icon_onlyPublish){sendEventPublish(name: "condition_icon_only", value: (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')}
    }
    if(getDataValue("forecastPoll") == "false"){
        updateDataValue("forecastPoll", "true")
    }
    PostPoll()
}
// >>>>>>>>>> End DarkSky Poll Routines <<<<<<<<<<
// <<<<<<<<<< Begin Post-Poll Routines >>>>>>>>>>
def PostPoll() {
    def sunRiseSet = parseJson(getDataValue("sunRiseSet")).results
    setDateTimeFormats(datetimeFormat)
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)   
/*  SunriseSunset Data Eements */    
    if(localSunrisePublish){  // don't bother setting these values if it's not enabled
        sendEvent(name: "tw_begin", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "sunriseTime", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "noonTime", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "sunsetTime", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "tw_end", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end).format(timeFormat, TimeZone.getDefault()))    
        sendEvent(name: "localSunset", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault())) // only needed for SmartTiles dashboard
        sendEvent(name: "localSunrise", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault())) // only needed for SmartTiles dashboard       
    }
/*  Weather-Display Data Elements */
	sendEvent(name: "humidity", value: getDataValue("humidity").toInteger())
	sendEvent(name: "pressure", value: String.format("%,4.1f", getDataValue("pressure").toBigDecimal()))
	sendEvent(name: "temperature", value: getDataValue("temperature").toBigDecimal())
    
    sendEventPublish(name: "alert", value: getDataValue("alert"))
    sendEventPublish(name: "betwixt", value: getDataValue("bwn"))
    if(chanceOfRainPublish){sendEvent(name: "chanceOfRain", value: getDataValue("percentPrecip") + '%')}
    sendEventPublish(name: "city", value: getDataValue("city"))
    sendEventPublish(name: "cloud", value: getDataValue("cloud").toInteger())
    if(condition_codePublish){
        sendEvent(name: "condition_code", value: getDataValue("condition_code"))
        sendEvent(name: "weatherIcon", value: getDataValue("condition_code"))
    }
    if(condition_textPublish){
        sendEvent(name: "condition_text", value: getDataValue("condition_text"))
        sendEvent(name: "weather", value: getDataValue("condition_text"))
    }
    sendEventPublish(name: "country", value: getDataValue("country"))
    sendEventPublish(name: "dewpoint", value: getDataValue("dewpoint"))
    sendEventPublish(name: "feelsLike", value: getDataValue("feelsLike").toBigDecimal())
    sendEventPublish(name: "forecastIcon", value: getDataValue("condition_code"))
    if(illuminancePublish){sendEvent(name: "illuminance", value: getDataValue("illuminance"))}
    if(illuminatedPublish){sendEvent(name: "illuminated", value: getDataValue("illuminated"))}
    sendEventPublish(name: "is_day", value: getDataValue("is_day"))
    if(obspollPublish){  // don't bother setting these values if it's not enabled
        sendEvent(name: "last_observation_Station", value: new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("sotime")).format(dateFormat, TimeZone.getDefault()) + ", " + new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("sotime")).format(timeFormat, TimeZone.getDefault()))
	    sendEvent(name: "last_poll_Station", value: new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("sutime")).format(dateFormat, TimeZone.getDefault()) + ", " + new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("sutime")).format(timeFormat, TimeZone.getDefault()))
    	sendEvent(name: "last_poll_Forecast", value: new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("futime")).format(dateFormat, TimeZone.getDefault()) + ", " + new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("futime")).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "last_observation_Forecast", value: new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("fotime")).format(dateFormat, TimeZone.getDefault()) + ", " + new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("fotime")).format(timeFormat, TimeZone.getDefault()))
    }
    if(latPublish){ // don't bother setting these values if it's not enabled
        sendEvent(name: "loc_lat", value: getDataValue("loc_lat").toBigDecimal())
        sendEvent(name: "loc_lon", value: getDataValue("loc_lon").toBigDecimal())
    }
//    sendEventPublish(name: "lux", value: getDataValue("lux"))
    sendEventPublish(name: "moonPhase", value: getDataValue("moonPhase"))
    if(chanceOfRainPublish){sendEvent(name: "percentPrecip", value: getDataValue("percentPrecip"))}
    sendEventPublish(name: "precip_today", value: getDataValue("precip_today"))
    sendEventPublish(name: "solarradiation", value: getDataValue("solarradiation"))
    sendEventPublish(name: "state", value: getDataValue("state"))
    sendEventPublish(name: "UV", value: getDataValue("uv"))
    sendEventPublish(name: "vis", value: getDataValue("vis"))
    sendEvent(name: "wind", value: getDataValue("wind").toBigDecimal())
    sendEvent(name: "wind_degree", value: getDataValue("wind_degree"))
    sendEvent(name: "wind_direction", value: getDataValue("wind_direction"))
    sendEvent(name: "wind_gust", value: getDataValue("wind_gust").toBigDecimal())
    sendEvent(name: "wind_string", value: getDataValue("wind_string"))
    sendEventPublish(name: "wslocation", value: getDataValue("wslocation"))
/* Forecast Data Elements	*/
    sendEventPublish(name: "forecast_code", value: getDataValue("forecast_code"))
    sendEventPublish(name: "forecast_text", value: getDataValue("forecast_text"))
    if(fcstHighLowPublish){ // don't bother setting these values if it's not enabled
        sendEvent(name: "forecastHigh", value: getDataValue("forecastHigh").toBigDecimal())
    	sendEvent(name: "forecastLow", value: getDataValue("forecastLow").toBigDecimal())
    }
    if(precipExtendedPublish){ // don't bother setting these values if it's not enabled
        sendEvent(name: "rainDayAfterTomorrow", value: getDataValue("rainDayAfterTomorrow") + (extSource.toInteger() == 3 ? '%' : (isRainMetric ? ' mm' : ' inches')))	
    	sendEvent(name: "rainTomorrow", value: getDataValue("rainTomorrow") + (extSource.toInteger() == 3 ? '%' : (isRainMetric ? ' mm' : ' inches')))
    }  
    if(summarymessagePublish){ // don't bother setting these values if it's not enabled
        Summary_last_poll_time = (sutime > futime ? new Date().parse("EEE MMM dd HH:mm:ss z yyyy", "${sutime}").format(timeFormat, TimeZone.getDefault()) : new Date().parse("EEE MMM dd HH:mm:ss z yyyy", "${futime}").format(timeFormat, TimeZone.getDefault()))
        Summary_last_poll_date = (sutime > futime ? new Date().parse("EEE MMM dd HH:mm:ss z yyyy", "${sutime}").format(dateFormat, TimeZone.getDefault()) : new Date().parse("EEE MMM dd HH:mm:ss z yyyy", "${futime}").format(dateFormat, TimeZone.getDefault()))
	
        if(extSource.toInteger() != 1){Summary_forecastTemp = " with a high of " + getDataValue("forecastHigh") + (isFahrenheit ? '°F' : '°C') + " and a low of " + getDataValue("forecastLow") + (isFahrenheit ? '°F. ' : '°C. ')}
        if(extSource.toInteger() == 3){Summary_precip = "There is a " + getDataValue("percentPrecip") + "% chance of precipitation. "} 
        if(extSource.toInteger() != 3){Summary_precip = ""}
        if(extSource.toInteger() != 3){
            mtprecip = 'N/A'
        }else{
            mtprecip = getDataValue("percentPrecip") + '%'
        }
        if(extSource.toInteger() != 1){Summary_vis = "Visibility is around " + getDataValue("vis") + (isDistanceMetric ? " kilometers." : " miles. ")}
        SummaryMessage(summaryType, Summary_last_poll_date, Summary_last_poll_time, Summary_forecastTemp, Summary_precip, Summary_vis)
    }
//  <<<<<<<<<< Begin Built mytext >>>>>>>>>> 
    if(mytilePublish){ // don't bother setting these values if it's not enabled
    	iconClose = (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : "")
        alertStyleOpen = (!possAlert ?  '' : '<span style=\"font-size:0.65em;line-height=65%;\">')
        alertStyleClose = (!possAlert ? '</span>' : ' | </span><span style=\"font-style:italic;\">' + getDataValue("alert") + "</span>" )
        alertsize = alertStyleOpen.length() + alertStyleClose.length()   
        PathMultiplier = (getDataValue("precip_today").toBigDecimal() > 0.00 && getDataValue("precip_today") != null && getDataValue("precip_today") != "") ? 7 : 6
        fileLength = ((getDataValue("iconLocation") + iconClose).length() + 10) * PathMultiplier
        myTileRawLength = getDataValue("wslocation").length() 
        myTileRawLength += getDataValue("condition_text").length() 
        myTileRawLength += getDataValue("temperature").length() 
        myTileRawLength += getDataValue("condition_icon_url").length()
        myTileRawLength += getDataValue("feelsLike").length()
        myTileRawLength += getDataValue("wind_bft_icon").length() 
        myTileRawLength += getDataValue("wind_direction").length() 
        myTileRawLength += getDataValue("pressure").length() 
        myTileRawLength += getDataValue("humidity").length() 
        myTileRawLength += alertsize
        precipLength = getDataValue("percentPrecip").length() + (getDataValue("precip_today").toBigDecimal() > 0 ? 18 : 0)
        formatLength = 265
//   82 first line formatting only; 4 second line; 63 thrird line; 48 fourth line; 31 fifth line; 35 sxith line  =~265
        TotalLength = fileLength + myTileRawLength + precipLength + formatLength
        LOGINFO("TotalLength: " + TotalLength)
        if(getDataValue("wind_gust").toBigDecimal() < 1.00 && getDataValue("wind_gust") != null && getDataValue("wind_gust") != "") {
            def BigDecimal wgust = 0.00
        } else {
            def BigDecimal wgust = getDataValue("wind_gust").toBigDecimal()
        }
        if(TotalLength < 1024){
            mytext = '<div style=\"text-align:center;display:inline;margin-top:0em;margin-bottom:0em;\">' + getDataValue("wslocation") + '</div><br>'
            mytext+= alertStyleOpen + getDataValue("condition_text") + alertStyleClose + '<br>'
            mytext+= getDataValue("temperature") + (isFahrenheit ? '°F ' : '°C ') + '<img style=\"height:2.0em\" src=' + getDataValue("condition_icon_url") + '>' + '<span style= \"font-size:.65em;\"> Feels like ' + getDataValue("feelsLike") + (isFahrenheit ? '°F' : '°C') + '</span><br>'
            mytext+= '<div style=\"font-size:0.5em;line-height=50%;\">' + '<img src=' + getDataValue("iconLocation") + getDataValue("wind_bft_icon") + iconClose + '>' + getDataValue("wind_direction") + " "
            mytext+= getDataValue("wind").toBigDecimal() < 1.0 ? 'calm' : "@ " + getDataValue("wind") + (isDistanceMetric ? ' KPH' : ' MPH') 
            mytext+= ', gusts ' + ((wgust < 1.00) ? 'calm' :  "@ " + wgust.toString() + (isDistanceMetric ? ' KPH' : ' MPH')) + '<br>'
            mytext+= '<img src=' + getDataValue("iconLocation") + 'wb.png' + iconClose + '>' + String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) + (isPressureMetric ? 'MB' : 'IN') + '  <img src=' + getDataValue("iconLocation") + 'wh.png' + iconClose + '>'
            mytext+= getDataValue("humidity") + '%  ' + '<img src=' + getDataValue("iconLocation") + 'wu.png' + iconClose + '>' + mtprecip
            mytext+= (getDataValue("precip_today").toBigDecimal() > 0.00 ? '  <img src=' + getDataValue("iconLocation") + 'wr.png' + iconClose + '>' + getDataValue("precip_today") + (isRainMetric ? ' mm' : ' inches') : '') + '<br>'
            mytext+= '<img src=' + getDataValue("iconLocation") + 'wsr.png' + iconClose + '>' + getDataValue("localSunrise") + '     <img src=' + getDataValue("iconLocation") + 'wss.png' + iconClose + '>' + getDataValue("localSunset") + '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Updated:&nbsp;' + Summary_last_poll_time + '</div>'
            LOGINFO("mytext: ${mytext}")
        } else {
            alertStyleOpen = ''
            alertStyleClose = (!possAlert ? '' : ' | ' + alert )
            mytext = getDataValue("wslocation") + "<br>"
            mytext+= alertStyleOpen + getDataValue("condition_text") + alertStyleClose + '<br>'
            mytext+= getDataValue("temperature") + (isFahrenheit ? '°F ' : '°C ') + '<img src=' + getDataValue("condition_icon_url") + '>' + ' Feels like ' + getDataValue("feelsLike") + (isFahrenheit ? '°F' : '°C')+ '<br>'
            mytext+= getDataValue("wind_direction") + " " + (getDataValue("wind").toBigDecimal() < 1 ? 'calm' : "@ " + getDataValue("wind") + (isDistanceMetric ? ' KPH' : ' MPH') + ', gusts ' + (getDataValue("wind_gust").toBigDecimal() < 1 ? 'calm' : "@ " + getDataValue("wind_gust") + (isDistanceMetric ? " KPH" : " MPH"))) + '<br>'
            mytext+= 'P:' + String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) + (isPressureMetric ? 'MB' : 'IN') + "   H: " + getDataValue("humidity") + '%  ' + "   Chance Precip: " + mtprecip + (getDataValue("precip_today").toBigDecimal() > 0 ? "     Precip Today: " + getDataValue("precip_today") + (isRainMetric ? ' mm' : ' inches') : '') + '<br>'
            mytext+= 'rise ' + getDataValue("localSunrise") + '     set ' + getDataValue("localSunset")
            if("${mytext}".length() > 1024){
                log.error "myTile length is too long at: " + "${mytext}".length() + " and was truncated."
                mytext = "${mytext}".take(1023)
            }
            LOGINFO("mytext: ${mytext}")
        }
        sendEvent(name: "myTile", value: mytext)
    }
}
// >>>>>>>>>> End Post-Poll Routines <<<<<<<<<<
def updated()   {
	initialize()  // includes an unsubscribe()
    updateDataValue("forecastPoll", "false")
    if (settingEnable) runIn(2100,settingsOff)  // "roll up" (hide) the condition selectors after 35 min
    runIn(5, pollWD)
}
def initialize() {
    state.clear()
    unschedule()
    state.driverVersion = "0.0.2"    // ************************* Update as required *************************************
	state.driverNameSpace = "Matthew"
    logSet = (settings?.logSet ?: false)
	extSource = (settings?.extSource ?: 1).toInteger()
    pollIntervalStation = (settings?.pollIntervalStation ?: "3 Hours")
    stationLatHemi =  (settings?.stationLatHemi ?: "North")
    stationLongHemi =  (settings?.stationLongHemi ?: "West")
    pollLocationStation = (settings?.pollLocationStation ?: "http://")
    pollIntervalForecast = (settings?.pollIntervalForecast ?: "3 Hours")
    pollLocationForecast = (settings?.pollLocationForecast ?: "ZIP Code (APIXU) or Latitude,longitude (DarkSky)")
	datetimeFormat = (settings?.datetimeFormat ?: 1).toInteger()
    distanceFormat = (settings?.distanceFormat ?: "Miles (mph)")
    pressureFormat = (settings?.pressureFormat ?: "Inches")
    rainFormat = (settings?.rainFormat ?: "Inches")
    tempFormat = (settings?.tempFormat ?: "Fahrenheit (°F)")
	iconType = (settings?.iconType ?: false)
    updateDataValue("iconType", iconType ? 'true' : 'false')
    sourcefeelsLike = (settings?.sourcefeelsLike ?: false)
    sourceIllumination = (settings?.sourceIllumination ?: false)
    sourceImg = (settings?.sourceImg ?: false)
    sourceUV = (settings?.sourceUV ?: false)
    summaryType = (settings?.summaryType ?: false)
    iconLocation = (settings?.iconLocation ?: "https://raw.githubusercontent.com/Scottma61/WeatherIcons/master/")
    updateDataValue("iconLocation", iconLocation)
	if(pollLocationForecast.contains(',')==true) {
		loc_lat = pollLocationForecast.split(',')[0].toBigDecimal()
        updateDataValue("loc_lat", pollLocationForecast.split(',')[0].toBigDecimal().toString())
		loc_lon = pollLocationForecast.split(',')[1].toBigDecimal()
        updateDataValue("loc_lon", pollLocationForecast.split(',')[1].toBigDecimal().toString())
	}
    setDateTimeFormats(datetimeFormat)
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
    pollSunRiseSet()
	schedule("11 20 0/8 ? * * *", pollSunRiseSet)
    if(pollIntervalStation == "Manual Poll Only"){
        LOGINFO("MANUAL STATION POLLING ONLY")
    } else {
        pollIntervalStation = (settings?.pollIntervalStation ?: "3 Hours").replace(" ", "")
        if(pollIntervalStation != pollIntervalForecast){
            "runEvery${pollIntervalStation}"(pollWD)
            LOGINFO("pollIntervalStation: $pollIntervalStation")
        }
    }
	if(pollIntervalForecast == "Manual Poll Only"){
		LOGINFO("MANUAL FORECAST POLLING ONLY")
	} else {
        pollIntervalForecast = (settings?.pollIntervalForecast ?: "3 Hours").replace(" ", "")
        if (extSource.toInteger() == 1) {
            "runEvery${pollIntervalForecast}"(pollWD)
        } else if (extSource.toInteger() == 2) {
            "runEvery${pollIntervalForecast}"(pollXU)
        } else if (extSource.toInteger() == 3) {
            "runEvery${pollIntervalForecast}"(pollDS)
        }
	}
    return
}

def pollData() {
    pollWD()
    if (extSource.toInteger() == 2) {
        pollXU()
    } else if (extSource.toInteger() == 3) {
        pollDS()
    }
    return
}
// ************************************************************************************************

public setDateTimeFormats(formatselector){
    switch(formatselector) {
        case 1: DTFormat = "M/d/yyyy h:mm a";   dateFormat = "M/d/yyyy";   timeFormat = "h:mm a"; break;
        case 2: DTFormat = "M/d/yyyy HH:mm";    dateFormat = "M/d/yyyy";   timeFormat = "HH:mm";  break;
    	case 3: DTFormat = "MM/dd/yyyy h:mm a"; dateFormat = "MM/dd/yyyy"; timeFormat = "h:mm a"; break;
    	case 4: DTFormat = "MM/dd/yyyy HH:mm";  dateFormat = "MM/dd/yyyy"; timeFormat = "HH:mm";  break;
		case 5: DTFormat = "d/M/yyyy h:mm a";   dateFormat = "d/M/yyyy";   timeFormat = "h:mm a"; break;
    	case 6: DTFormat = "d/M/yyyy HH:mm";    dateFormat = "d/M/yyyy";   timeFormat = "HH:mm";  break;
    	case 7: DTFormat = "dd/MM/yyyy h:mm a"; dateFormat = "dd/MM/yyyy"; timeFormat = "h:mm a"; break;
        case 8: DTFormat = "dd/MM/yyyy HH:mm";  dateFormat = "dd/MM/yyyy"; timeFormat = "HH:mm";  break;
    	case 9: DTFormat = "yyyy/MM/dd HH:mm";  dateFormat = "yyyy/MM/dd"; timeFormat = "HH:mm";  break;
    	default: DTFormat = "M/d/yyyy h:mm a";  dateFormat = "M/d/yyyy";   timeFormat = "h:mm a"; break;
	}
    return
}
public setMeasurementMetrics(distFormat, pressFormat, precipFormat, temptFormat){
    isDistanceMetric = (distFormat == "Kilometers (kph)") ? true : false
    isPressureMetric = (pressFormat == "Millibar") ? true : false
    isRainMetric = (precipFormat == "Millimetres") ? true : false
    isFahrenheit = (temptFormat == "Fahrenheit (°F)") ? true : false
    return
}

def estimateLux(condition_code, cloud)     {	
	def lux = 0l
	def aFCC = true
	def l
	def bwn
	def sunRiseSet           = parseJson(getDataValue("sunRiseSet")).results
	def tZ                   = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	def lT                   = new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
	def localeMillis         = getEpoch(lT)
	def twilight_beginMillis = getEpoch(sunRiseSet.civil_twilight_begin)
	def sunriseTimeMillis    = getEpoch(sunRiseSet.sunrise)
	def noonTimeMillis       = getEpoch(sunRiseSet.solar_noon)
	def sunsetTimeMillis     = getEpoch(sunRiseSet.sunset)
	def twilight_endMillis   = getEpoch(sunRiseSet.civil_twilight_end)
	def twiStartNextMillis   = twilight_beginMillis + 86400000 // = 24*60*60*1000 --> one day in milliseconds
	def sunriseNextMillis    = sunriseTimeMillis + 86400000 
	def noonTimeNextMillis   = noonTimeMillis + 86400000 
	def sunsetNextMillis     = sunsetTimeMillis + 86400000
	def twiEndNextMillis     = twilight_endMillis + 86400000

	switch(localeMillis) { 
		case { it < twilight_beginMillis}: 
			bwn = "Fully Night Time" 
			lux = 5l
			break
		case { it < sunriseTimeMillis}:
			bwn = "between twilight and sunrise" 
			l = (((localeMillis - twilight_beginMillis) * 50f) / (sunriseTimeMillis - twilight_beginMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		case { it < noonTimeMillis}:
			bwn = "between sunrise and noon" 
			l = (((localeMillis - sunriseTimeMillis) * 10000f) / (noonTimeMillis - sunriseTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < sunsetTimeMillis}:
			bwn = "between noon and sunset" 
			l = (((sunsetTimeMillis - localeMillis) * 10000f) / (sunsetTimeMillis - noonTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < twilight_endMillis}:
			bwn = "between sunset and twilight" 
			l = (((twilight_endMillis - localeMillis) * 50f) / (twilight_endMillis - sunsetTimeMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		case { it < twiStartNextMillis}:
			bwn = "Fully Night Time" 
			lux = 5l
			break
		case { it < sunriseNextMillis}:
			bwn = "between twilight and sunrise" 
			l = (((localeMillis - twiStartNextMillis) * 50f) / (sunriseNextMillis - twiStartNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		case { it < noonTimeNextMillis}:
			bwn = "between sunrise and noon" 
			l = (((localeMillis - sunriseNextMillis) * 10000f) / (noonTimeNextMillis - sunriseNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < sunsetNextMillis}:
			bwn = "between noon and sunset" 
			l = (((sunsetNextMillis - localeMillis) * 10000f) / (sunsetNextMillis - noonTimeNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < twiEndNextMillis}:
			bwn = "between sunset and twilight" 
			l = (((twiEndNextMillis - localeMillis) * 50f) / (twiEndNextMillis - sunsetNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		default:
			bwn = "Fully Night Time" 
			lux = 5l
			aFCC = false
			break
	}

	def cC = condition_code
	def cCF = (!cloud || cloud=="") ? 0.998d : ((100 - (cloud.toInteger() / 3d)) / 100)

    if(aFCC){
		if(extSource.toInteger() == 1 && cloud !="" && cloud != null){
            cCF = ((100 - (cloud.toInteger() / 3d)) / 100)
            cCT = 'using cloud cover'
		} else if(extSource.toInteger() == 2 && cloud !="" && cloud != null){
			LUitem = LUTable.find{ it.xucode == condition_code && it.day == 1 }            
			if (LUitem && (condition_code != "unknown"))    {
				cCF = (LUitem ? LUitem.luxpercent : 0)
				cCT = (LUitem ? LUitem.wuphrase : 'unknown') + ' using cloud cover.'
            } else {
                cCF = 1.0
		        cCT = 'cloud not available now.'
            }
		} else if(extSource.toInteger() == 3 && cloud !="" && cloud != null){
			LUitem = LUTable.find{ it.wucode == condition_code && it.day == 1 }            
			if (LUitem && (condition_code != "unknown"))    {
				cCF = (LUitem ? LUitem.luxpercent : 0)
				cCT = (LUitem ? LUitem.wuphrase : 'unknown') + ' using cloud cover.'
            } else    {
                cCF = 1.0
		        cCT = 'cloud not available now.'
			}
        } else {
		    cCF = 1.0
		    cCT = 'cloud not available now.'
        }
    }    
	lux = (lux * cCF) as long
	LOGDEBUG("condition: $cC | condition factor: $cCF | condition text: $cCT| lux: $lux")
	return [lux, bwn]
}
def getEpoch (aTime) {
	def tZ = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	def localeTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", aTime, tZ)
	long localeMillis = localeTime.getTime()
	return (localeMillis)
}

public SummaryMessage(SType, Slast_poll_date, Slast_poll_time, SforecastTemp, Sprecip, Svis){   
    if(getDataValue("wind_gust").toBigDecimal() == 0 || getDataValue("wind_gust").toBigDecimal() < 1.00 || getDataValue("wind_gust")==null) {
        def BigDecimal wgust = 0.00
    } else {
        def BigDecimal wgust = getDataValue("wind_gust").toBigDecimal()
    }
    if(SType == true){
        wSum = "Weather summary for " + getDataValue("wslocation") + " updated at ${Slast_poll_time} on ${Slast_poll_date}. "
        wSum+= getDataValue("condition_text")
        wSum+= (!SforecastTemp || SforecastTemp=="") ? "" : "${SforecastTemp}"
        wSum+= "Humidity is " + getDataValue("humidity") + "% and the temperature is " + getDataValue("temperature") +  (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= "The temperature feels like it is " + getDataValue("feelsLike") + (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= "Wind: " + getDataValue("wind_string") + ", gusts: " + ((wgust < 1.00) ? "calm. " : "up to " + wgust.toString() + (isDistanceMetric ? ' KPH. ' : ' MPH. '))
        wSum+= Sprecip
        wSum+= Svis
        wSum+= (!getDataValue("alert") || getDataValue("alert")==null) ? "" : getDataValue("alert")
        sendEvent(name: "weatherSummary", value: wSum)
    } else {
        wSum = "${Scondition_text}"
        wSum+= ((!SforecastTemp || SforecastTemp=="") ? "" : "${SforecastTemp}")
        wSum+= " Humidity: " + getDataValue("humidity") + "%. Temperature: " + getDataValue("temperature") + (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= getDataValue("wind_string") + ", gusts: " + ((wgust == 0.00) ? "calm. " : "up to " + wgust + (isDistanceMetric ? ' KPH. ' : ' MPH. '))
		sendEvent(name: "weatherSummary", value: wSum)
	}
	return
}

public getDSImgName(wSummary, wCode){
    LOGINFO("getDSImgName Input: wSummary: " + wSummary + ", wCode: " + wCode + ",   is_day: " + getDataValue("is_day") + ",  iconLocation: " + getDataValue("iconLocation"))
    LUitem = LUTable.find{ it.wuphrase.toLowerCase() == "${wSummary}".toLowerCase() && it.day.toString() == getDataValue("is_day") }
	if(!LUitem){
		LUitem = LUTable.find{ it.wucode == wCode && it.day.toString() == getDataValue("is_day") }
	}
    LOGINFO("getDSImgName Result: image url: " + getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : ""))
	return (getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : ""))    
}
private getWUCodeName(wCode){
    LOGINFO("getWUCodeName Input: wCode: " + wCode + "  state.is_day: " + getDataValue("is_day"))
    LUitem = LUTable.find{ it.xucode == wCode && it.day.toString() == getDataValue("is_day") }    
    LOGINFO("getWUCodeName Result: APIXU Code: " + wCode + "WUCode: " + (LUitem ? LUitem.wucode : 'unknown'))
    return (LUitem ? LUitem.wucode : 'unknown')   
}
private getDSTextName(wSummary, wCode){
	LOGINFO("getDSTextName Input: wSummary: " + wSummary + ",  wCode: ${wCode}")
	if(!wCode || wCode == 'Empty'){
		LUitem = LUTable.find{ it.wucode.toLowerCase() == wSummary.toLowerCase() }
        LOGINFO("getDSTextName Result: wSummary: " + wSummary + "  DSTextName Return: " + (LUitem ? LUitem.wuphrase : 'unknown'))
	} else {
        LUitem = LUTable.find{ it.wuphrase.toLowerCase() == wSummary.toString().toLowerCase() }
		if(!LUitem){
			LOGINFO("getDSTextName Result: wCode: " + wCode + "  DSTextName Return: " + (LUitem ? LUitem.wuphrase : 'unknown'))
            LUitem = LUTable.find{ it.wucode.toLowerCase() == wCode.toLowerCase() }
		} else {
			LOGINFO("getDSTextName Result: wSummary: " + wSummary + "  DSTextName Return: " + (LUitem ? LUitem.wuphrase : 'unknown'))
		}
	}
	return (LUitem ? LUitem.wuphrase : 'unknown')   
}
public getImgName(wCode){
    LOGINFO("getImgName Input: wCode: " + wCode + "  state.is_day: " + getDataValue("is_day") + " iconLocation: " + getDataValue("iconLocation"))
    LUitem = LUTable.find{ (extSource.toInteger() == 1 ? it.wuphrase : (extSource.toInteger() == 2 ? it.xucode : it.wucode)) == wCode && it.day.toString() == getDataValue("is_day") }    
	LOGINFO("getImgName Result: image url: " + getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + "?raw=true")
    return (getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : ""))    
}
def logCheck(){
    if(logSet == true){
        log.info "Weather-Display Driver - INFO:  All Logging Enabled"
	} else {
        log.info "Weather-Display Driver - INFO:  Further Logging Disabled"
    }
    return
}
def LOGDEBUG(txt){
    try {
    	if(logSet == true){ log.debug("Weather-Display Driver - DEBUG:  ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
    return
}

def LOGINFO(txt){
    try {
    	if(logSet == true){log.info("Weather-Display Driver - INFO:  ${txt}") }
    } catch(ex) {
    	log.error("LOGINFO unable to output requested data!")
    }
    return
}
def settingsOff(){
	log.warn "Settings disabled..."
	device.updateSetting("settingEnable",[value:"false",type:"bool"])
}


def sendEventPublish(evt)	{
// 	Purpose: Attribute sent to DB if selected	
	if (this[evt.name + "Publish"]) {
		sendEvent(name: evt.name, value: evt.value, descriptionText: evt.descriptionText, unit: evt.unit, displayed: evt.displayed);
		if (debugOutput) log.debug "$evt.name" //: $evt.name, $evt.value $evt.unit"
	}
}

@Field final List    LUTable =     [
	 [xucode: 1000, wuphrase: 'Clear', wucode: 'sunny', day: 1, img: '32.png', luxpercent: 1],   // DAY: Sunny - Clear
     [xucode: 1000, wuphrase: 'Clear', wucode: 'clear', day: 1, img: '32.png', luxpercent: 1],   // DAY: Sunny - Clear
     [xucode: 1003, wuphrase: 'Partly Cloudy', wucode: 'partlycloudy', day: 1, img: '30.png', luxpercent: 0.8],   // DAY: Partly cloudy
     [xucode: 1003, wuphrase: 'Scattered Clouds', wucode: 'partlycloudy', day: 1, img: '30.png', luxpercent: 0.8],   // DAY: Partly cloudy - Scattered Clouds
     [xucode: 1006, wuphrase: 'Mostly Cloudy', wucode: 'cloudy', day: 1, img: '26.png', luxpercent: 0.6],   // DAY: Cloudy - Mostly Cloudy
     [xucode: 1009, wuphrase: 'Overcast', wucode: 'cloudy', day: 1, img: '28.png', luxpercent: 0.6],   // DAY: Overcast
     [xucode: 1030, wuphrase: 'Hazy', wucode: 'hazy', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Mist
     [xucode: 1063, wuphrase: 'Rain', wucode: 'rain', day: 1, img: '39.png', luxpercent: 0.5],   // DAY: Patchy rain possible - Rain
     [xucode: 1066, wuphrase: 'Light Thunderstorms and Snow', wucode: 'chancesnow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Patchy snow possible - Light Thunderstorms and Snow
     [xucode: 1069, wuphrase: 'Ice Pellets', wucode: 'sleet', day: 1, img: '17.png', luxpercent: 0.5],   // DAY: Patchy sleet possible - Ice Pellets
     [xucode: 1072, wuphrase: 'Light Freezing Drizzle', wucode: 'sleet', day: 1, img: '6.png', luxpercent: 0.3],   // DAY: Patchy freezing drizzle possible - Light Freezing Drizzle
     [xucode: 1087, wuphrase: 'Thunderstorm', wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Thundery outbreaks possible - Thunderstorm
     [xucode: 1216, wuphrase: 'Snow', wucode: 'snow', day: 1, img: '7.png', luxpercent: 0.3],   // DAY: Patchy moderate snow - Snow
	 [xucode: 1114, wuphrase: 'Blowing Snow', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow
     [xucode: 1114, wuphrase: 'Heavy Blowing Snow', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Heavy Blowing Snow
     [xucode: 1114, wuphrase: 'Heavy Low Drifting Snow', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Heavy Low Drifting Snow
     [xucode: 1114, wuphrase: 'Heavy Snow Blowing Snow Mist', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Heavy Snow Blowing Snow Mist
     [xucode: 1114, wuphrase: 'Light Blowing Snow', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Light Blowing Snow
     [xucode: 1114, wuphrase: 'Light Low Drifting Snow', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Light Low Drifting Snow
     [xucode: 1114, wuphrase: 'Light Snow Blowing Snow Mist', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Light Snow Blowing Snow Mist
     [xucode: 1114, wuphrase: 'Low Drifting Snow', wucode: 'snow', day: 1, img: '15.png', luxpercent: 0.3],   // DAY: Blowing snow - Low Drifting Snow
     [xucode: 1117, wuphrase: 'Heavy Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Blizzard - Heavy Snow
     [xucode: 1135, wuphrase: 'Fog', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog
     [xucode: 1135, wuphrase: 'Fog Patches', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Fog Patches
     [xucode: 1135, wuphrase: 'Hazy', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Haze
     [xucode: 1135, wuphrase: 'Heavy Fog', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Heavy Fog
     [xucode: 1135, wuphrase: 'Heavy Fog Patches', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Heavy Fog Patches
     [xucode: 1135, wuphrase: 'Light Fog', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Light Fog
     [xucode: 1135, wuphrase: 'Light Fog Patches', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Light Fog Patches
     [xucode: 1135, wuphrase: 'Mist', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Mist
     [xucode: 1135, wuphrase: 'Partial Fog', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Partial Fog
     [xucode: 1135, wuphrase: 'Shallow Fog', wucode: 'fog', day: 1, img: '20.png', luxpercent: 0.2],   // DAY: Fog - Shallow Fog
     [xucode: 1147, wuphrase: 'Freezing Fog', wucode: 'fog', day: 1, img: '21.png', luxpercent: 0.2],   // DAY: Freezing fog
     [xucode: 1147, wuphrase: 'Heavy Freezing Fog', wucode: 'fog', day: 1, img: '21.png', luxpercent: 0.2],   // DAY: Freezing fog - Heavy Freezing Fog
     [xucode: 1147, wuphrase: 'Light Freezing Fog', wucode: 'fog', day: 1, img: '21.png', luxpercent: 0.2],   // DAY: Freezing fog - Light Freezing Fog
     [xucode: 1147, wuphrase: 'Patches of Fog', wucode: 'fog', day: 1, img: '21.png', luxpercent: 0.2],   // DAY: Freezing fog - Patches of Fog
     [xucode: 1150, wuphrase: 'Light Drizzle', wucode: 'rain', day: 1, img: '9.png', luxpercent: 0.5],   // DAY: Patchy light drizzle - Light Drizzle
     [xucode: 1153, wuphrase: 'Drizzle', wucode: 'rain', day: 1, img: '9.png', luxpercent: 0.5],   // DAY: Light drizzle - Drizzle
     [xucode: 1153, wuphrase: 'Light Drizzle', wucode: 'rain', day: 1, img: '9.png', luxpercent: 0.5],   // DAY: Light drizzle
     [xucode: 1153, wuphrase: 'Light Mist', wucode: 'rain', day: 1, img: '9.png', luxpercent: 0.5],   // DAY: Light drizzle - Light Mist
     [xucode: 1153, wuphrase: 'Light Rain Mist', wucode: 'rain', day: 1, img: '9.png', luxpercent: 0.5],   // DAY: Light drizzle - Light Rain Mist
     [xucode: 1153, wuphrase: 'Rain Mist', wucode: 'rain', day: 1, img: '9.png', luxpercent: 0.5],   // DAY: Light drizzle - Rain Mist
     [xucode: 1168, wuphrase: 'Freezing Drizzle', wucode: 'sleet', day: 1, img: '8.png', luxpercent: 0.3],   // DAY: Freezing drizzle
     [xucode: 1168, wuphrase: 'Heavy Freezing Drizzle', wucode: 'sleet', day: 1, img: '6.png', luxpercent: 0.3],   // DAY: Freezing drizzle - Heavy Freezing Drizzle
     [xucode: 1168, wuphrase: 'Light Freezing Drizzle', wucode: 'sleet', day: 1, img: '8.png', luxpercent: 0.3],   // DAY: Freezing drizzle - Light Freezing Drizzle
     [xucode: 1171, wuphrase: 'Heavy Freezing Drizzle', wucode: 'sleet', day: 1, img: '6.png', luxpercent: 0.3],   // DAY: Heavy freezing drizzle
     [xucode: 1180, wuphrase: 'Light Rain', wucode: 'rain', day: 1, img: '11.png', luxpercent: 0.5],   // DAY: Patchy light rain - Light Rain
     [xucode: 1183, wuphrase: 'Heavy Mist', wucode: 'rain', day: 1, img: '11.png', luxpercent: 0.5],   // DAY: Light rain - Heavy Mist
     [xucode: 1183, wuphrase: 'Heavy Rain Mist', wucode: 'rain', day: 1, img: '11.png', luxpercent: 0.5],   // DAY: Light rain - Heavy Rain Mist
     [xucode: 1183, wuphrase: 'Light Rain', wucode: 'rain', day: 1, img: '11.png', luxpercent: 0.5],   // DAY: Light rain
     [xucode: 1186, wuphrase: 'Rain', wucode: 'rain', day: 1, img: '39.png', luxpercent: 0.5],   // DAY: Moderate rain at times - Rain
     [xucode: 1189, wuphrase: 'Heavy Drizzle', wucode: 'rain', day: 1, img: '5.png', luxpercent: 0.5],   // DAY: Moderate rain - Heavy Drizzle
     [xucode: 1189, wuphrase: 'Rain', wucode: 'rain', day: 1, img: '5.png', luxpercent: 0.5],   // DAY: Moderate rain - Rain
     [xucode: 1192, wuphrase: 'Heavy Rain', wucode: 'rain', day: 1, img: '40.png', luxpercent: 0.5],   // DAY: Heavy rain at times - Heavy Rain
     [xucode: 1195, wuphrase: 'Heavy Rain', wucode: 'rain', day: 1, img: '40.png', luxpercent: 0.5],   // DAY: Heavy rain
     [xucode: 1198, wuphrase: 'Light Freezing Rain', wucode: 'sleet', day: 1, img: '6.png', luxpercent: 0.3],   // DAY: Light freezing rain
     [xucode: 1201, wuphrase: 'Heavy Freezing Rain', wucode: 'rain', day: 1, img: '6.png', luxpercent: 0.5],   // DAY: Moderate or heavy freezing rain - Heavy Freezing Rain
     [xucode: 1204, wuphrase: 'Hail', wucode: 'sleet', day: 1, img: '35.png', luxpercent: 0.5],   // DAY: Light sleet - Hail
     [xucode: 1204, wuphrase: 'Light Hail', wucode: 'sleet', day: 1, img: '35.png', luxpercent: 0.5],   // DAY: Light sleet - Light Hail
     [xucode: 1204, wuphrase: 'Light Ice Crystals', wucode: 'sleet', day: 1, img: '25.png', luxpercent: 0.5],   // DAY: Light sleet - Light Ice Crystals
     [xucode: 1204, wuphrase: 'Light Ice Pellets', wucode: 'sleet', day: 1, img: '35.png', luxpercent: 0.5],   // DAY: Light sleet - Light Ice Pellets
     [xucode: 1204, wuphrase: 'Light Snow Grains', wucode: 'sleet', day: 1, img: '35.png', luxpercent: 0.5],   // DAY: Light sleet - Light Snow Grains
     [xucode: 1204, wuphrase: 'Small Hail', wucode: 'sleet', day: 1, img: '35.png', luxpercent: 0.5],   // DAY: Light sleet - Small Hail
     [xucode: 1207, wuphrase: 'Heavy Ice Crystals', wucode: 'sleet', day: 1, img: '25.png', luxpercent: 0.5],   // DAY: Moderate or heavy sleet - Heavy Ice Crystals
     [xucode: 1210, wuphrase: 'Light Snow', wucode: 'snow', day: 1, img: '13.png', luxpercent: 0.3],   // DAY: Patchy light snow - Light Snow
     [xucode: 1213, wuphrase: 'Light Snow', wucode: 'snow', day: 1, img: '14.png', luxpercent: 0.3],   // DAY: Light snow
     [xucode: 1219, wuphrase: 'Snow', wucode: 'snow', day: 1, img: '7.png', luxpercent: 0.3],   // DAY: Moderate snow - Snow
     [xucode: 1222, wuphrase: 'Heavy Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Patchy heavy snow - Heavy Snow
     [xucode: 1225, wuphrase: 'Heavy Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Heavy snow
     [xucode: 1237, wuphrase: 'Ice Crystals', wucode: 'sleet', day: 1, img: '17.png', luxpercent: 0.5],   // DAY: Ice pellets - Ice Crystals
     [xucode: 1237, wuphrase: 'Ice Pellets', wucode: 'sleet', day: 1, img: '17.png', luxpercent: 0.5],   // DAY: Ice pellets
     [xucode: 1237, wuphrase: 'Snow Grains', wucode: 'sleet', day: 1, img: '17.png', luxpercent: 0.5],   // DAY: Ice pellets - Snow Grains
     [xucode: 1240, wuphrase: 'Light Rain Showers', wucode: 'rain', day: 1, img: '10.png', luxpercent: 0.5],   // DAY: Light rain shower - Light Rain Showers
     [xucode: 1243, wuphrase: 'Heavy Rain Showers', wucode: 'rain', day: 1, img: '12.png', luxpercent: 0.5],   // DAY: Moderate or heavy rain shower - Heavy Rain Showers
     [xucode: 1243, wuphrase: 'Rain Showers', wucode: 'rain', day: 1, img: '12.png', luxpercent: 0.5],   // DAY: Moderate or heavy rain shower - Rain Showers
     [xucode: 1246, wuphrase: 'Heavy Rain Showers', wucode: 'rain', day: 1, img: '12.png', luxpercent: 0.5],   // DAY: Torrential rain shower - Heavy Rain Showers
     [xucode: 1249, wuphrase: 'Light Thunderstorms with Hail', wucode: 'sleet', day: 1, img: '5.png', luxpercent: 0.5],   // DAY: Light sleet showers - Light Thunderstorms with Hail
     [xucode: 1252, wuphrase: 'Freezing Rain', wucode: 'sleet', day: 1, img: '18.png', luxpercent: 0.5],   // DAY: Moderate or heavy sleet showers - Freezing Rain
     [xucode: 1252, wuphrase: 'Heavy Small Hail Showers', wucode: 'sleet', day: 1, img: '18.png', luxpercent: 0.5],   // DAY: Moderate or heavy sleet showers - Heavy Small Hail Showers
     [xucode: 1252, wuphrase: 'Heavy Snow Grains', wucode: 'sleet', day: 1, img: '18.png', luxpercent: 0.5],   // DAY: Moderate or heavy sleet showers - Heavy Snow Grains
     [xucode: 1252, wuphrase: 'Ice Pellet Showers', wucode: 'sleet', day: 1, img: '18.png', luxpercent: 0.5],   // DAY: Moderate or heavy sleet showers - Ice Pellet Showers
     [xucode: 1252, wuphrase: 'Small Hail Showers', wucode: 'sleet', day: 1, img: '18.png', luxpercent: 0.5],   // DAY: Moderate or heavy sleet showers - Small Hail Showers
     [xucode: 1255, wuphrase: 'Light Snow Showers', wucode: 'snow', day: 1, img: '16.png', luxpercent: 0.3],   // DAY: Light snow showers
     [xucode: 1258, wuphrase: 'Heavy Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Heavy Snow
     [xucode: 1258, wuphrase: 'Heavy Snow Showers', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Heavy Snow Showers
     [xucode: 1258, wuphrase: 'Heavy Thunderstorms and Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Heavy Thunderstorms and Snow
     [xucode: 1258, wuphrase: 'Snow Blowing Snow Mist', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Snow Blowing Snow Mist
     [xucode: 1258, wuphrase: 'Snow Showers', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Snow Showers
     [xucode: 1258, wuphrase: 'Thunderstorms and Ice Pellets', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Thunderstorms and Ice Pellets
     [xucode: 1258, wuphrase: 'Thunderstorms and Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow showers - Thunderstorms and Snow
     [xucode: 1261, wuphrase: 'Light Hail Showers', wucode: 'snow', day: 1, img: '8.png', luxpercent: 0.3],   // DAY: Light showers of ice pellets - Light Hail Showers
     [xucode: 1261, wuphrase: 'Light Ice Pellet Showers', wucode: 'snow', day: 1, img: '8.png', luxpercent: 0.3],   // DAY: Light showers of ice pellets - Light Ice Pellet Showers
     [xucode: 1261, wuphrase: 'Light Small Hail Showers', wucode: 'snow', day: 1, img: '8.png', luxpercent: 0.3],   // DAY: Light showers of ice pellets - Light Small Hail Showers
     [xucode: 1261, wuphrase: 'Light Thunderstorms with Small Hail', wucode: 'snow', day: 1, img: '8.png', luxpercent: 0.3],   // DAY: Light showers of ice pellets - Light Thunderstorms with Small Hail
     [xucode: 1264, wuphrase: 'Hail Showers', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Hail Showers
     [xucode: 1264, wuphrase: 'Heavy Hail', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Hail
     [xucode: 1264, wuphrase: 'Heavy Hail Showers', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Hail Showers
     [xucode: 1264, wuphrase: 'Heavy Ice Crystals', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Ice Crystals
     [xucode: 1264, wuphrase: 'Heavy Ice Pellet Showers', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Ice Pellet Showers
     [xucode: 1264, wuphrase: 'Heavy Ice Pellets', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Ice Pellets
     [xucode: 1264, wuphrase: 'Heavy Thunderstorms and Ice Pellets', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Thunderstorms and Ice Pellets
     [xucode: 1264, wuphrase: 'Heavy Thunderstorms with Hail', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Thunderstorms with Hail
     [xucode: 1264, wuphrase: 'Heavy Thunderstorms with Small Hail', wucode: 'sleet', day: 1, img: '4.png', luxpercent: 0.5],   // DAY: Moderate or heavy showers of ice pellets - Heavy Thunderstorms with Small Hail
     [xucode: 1264, wuphrase: 'Thunderstorms with Small Hail', wucode: 'sleet', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Moderate or heavy showers of ice pellets - Thunderstorms with Small Hail
     [xucode: 1273, wuphrase: 'Light Thunderstorm', wucode: 'chancetstorms', day: 1, img: '37.png', luxpercent: 0.2],   // DAY: Patchy light rain with thunder - Light Thunderstorm
     [xucode: 1273, wuphrase: 'Light Thunderstorms and Rain', wucode: 'chancetstorms', day: 1, img: '37.png', luxpercent: 0.2],   // DAY: Patchy light rain with thunder - Light Thunderstorms and Rain
     [xucode: 1276, wuphrase: 'Heavy Thunderstorm', wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Moderate or heavy rain with thunder - Heavy Thunderstorm
     [xucode: 1276, wuphrase: 'Heavy Thunderstorms and Rain', wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Moderate or heavy rain with thunder - Heavy Thunderstorms and Rain
     [xucode: 1276, wuphrase: 'Thunderstorm', wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Moderate or heavy rain with thunder - Thunderstorm
     [xucode: 1276, wuphrase: 'Thunderstorms and Rain', wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Moderate or heavy rain with thunder - Thunderstorms and Rain
     [xucode: 1276, wuphrase: 'Thunderstorms with Hail', wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3],   // DAY: Moderate or heavy rain with thunder - Thunderstorms with Hail
     [xucode: 1279, wuphrase: 'Light Thunderstorms and Ice Pellets', wucode: 'chancesnow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Patchy light snow with thunder - Light Thunderstorms and Ice Pellets
     [xucode: 1279, wuphrase: 'Light Thunderstorms and Snow', wucode: 'chancesnow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Patchy light snow with thunder - Light Thunderstorms and Snow
     [xucode: 1282, wuphrase: 'Thunderstorms and Snow', wucode: 'snow', day: 1, img: '41.png', luxpercent: 0.3],   // DAY: Moderate or heavy snow with thunder - Thunderstorms and Snow
     [xucode: 1000, wuphrase: 'Breezy', wucode: 'breezy', day: 1, img: '22.png', luxpercent: 1],   // DAY: Breezy
     [xucode: 1000, wuphrase: 'Clear', wucode: 'nt_clear', day: 0, img: '31.png', luxpercent: 0],   // NIGHT: Clear
     [xucode: 1003, wuphrase: 'Partly Cloudy', wucode: 'nt_partlycloudy', day: 0, img: '29.png', luxpercent: 0],   // NIGHT: Partly cloudy
     [xucode: 1003, wuphrase: 'Scattered Clouds', wucode: 'nt_partlycloudy', day: 0, img: '29.png', luxpercent: 0],   // NIGHT: Partly cloudy - Scattered Clouds
     [xucode: 1006, wuphrase: 'Mostly Cloudy', wucode: 'nt_cloudy', day: 0, img: '26.png', luxpercent: 0],   // NIGHT: Cloudy - Mostly Cloudy
     [xucode: 1009, wuphrase: 'Overcast', wucode: 'nt_cloudy', day: 0, img: '27.png', luxpercent: 0],   // NIGHT: Overcast
     [xucode: 1030, wuphrase: 'Hazy', wucode: 'nt_hazy', day: 0, img: '21.png', luxpercent: 0],   // NIGHT: Mist
     [xucode: 1063, wuphrase: 'Rain', wucode: 'nt_rain', day: 0, img: '45.png', luxpercent: 0],   // NIGHT: Patchy rain possible - Rain
     [xucode: 1066, wuphrase: 'Light Thunderstorms and Snow', wucode: 'nt_chancesnow', day: 0, img: '46.png', luxpercent: 0],   // NIGHT: Patchy snow possible - Light Thunderstorms and Snow
     [xucode: 1069, wuphrase: 'Ice Pellets', wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Patchy sleet possible - Ice Pellets
     [xucode: 1072, wuphrase: 'Light Freezing Drizzle', wucode: 'nt_sleet', day: 0, img: '6.png', luxpercent: 0],   // NIGHT: Patchy freezing drizzle possible - Light Freezing Drizzle
     [xucode: 1087, wuphrase: 'Thunderstorm', wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0],   // NIGHT: Thundery outbreaks possible - Thunderstorm
     [xucode: 1216, wuphrase: 'Snow', wucode: 'nt_snow', day: 0, img: '46.png', luxpercent: 0],   // NIGHT: Patchy moderate snow - Snow
	 [xucode: 1114, wuphrase: 'Blowing Snow', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow
     [xucode: 1114, wuphrase: 'Heavy Blowing Snow', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Heavy Blowing Snow
     [xucode: 1114, wuphrase: 'Heavy Low Drifting Snow', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Heavy Low Drifting Snow
     [xucode: 1114, wuphrase: 'Heavy Snow Blowing Snow Mist', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Heavy Snow Blowing Snow Mist
     [xucode: 1114, wuphrase: 'Light Blowing Snow', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Light Blowing Snow
     [xucode: 1114, wuphrase: 'Light Low Drifting Snow', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Light Low Drifting Snow
     [xucode: 1114, wuphrase: 'Light Snow Blowing Snow Mist', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Light Snow Blowing Snow Mist
     [xucode: 1114, wuphrase: 'Low Drifting Snow', wucode: 'nt_snow', day: 0, img: '14.png', luxpercent: 0],   // NIGHT: Blowing snow - Low Drifting Snow
     [xucode: 1117, wuphrase: 'Heavy Snow', wucode: 'nt_snow', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Blizzard - Heavy Snow
     [xucode: 1135, wuphrase: 'Fog', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog
     [xucode: 1135, wuphrase: 'Fog Patches', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Fog Patches
     [xucode: 1135, wuphrase: 'Hazy', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Haze
     [xucode: 1135, wuphrase: 'Heavy Fog', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Heavy Fog
     [xucode: 1135, wuphrase: 'Heavy Fog Patches', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Heavy Fog Patches
     [xucode: 1135, wuphrase: 'Light Fog', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Light Fog
     [xucode: 1135, wuphrase: 'Light Fog Patches', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Light Fog Patches
     [xucode: 1135, wuphrase: 'Mist', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Mist
     [xucode: 1135, wuphrase: 'Partial Fog', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Partial Fog
     [xucode: 1135, wuphrase: 'Shallow Fog', wucode: 'nt_fog', day: 0, img: '20.png', luxpercent: 0],   // NIGHT: Fog - Shallow Fog
     [xucode: 1147, wuphrase: 'Freezing Fog', wucode: 'nt_fog', day: 0, img: '21.png', luxpercent: 0],   // NIGHT: Freezing fog
     [xucode: 1147, wuphrase: 'Heavy Freezing Fog', wucode: 'nt_fog', day: 0, img: '21.png', luxpercent: 0],   // NIGHT: Freezing fog - Heavy Freezing Fog
     [xucode: 1147, wuphrase: 'Light Freezing Fog', wucode: 'nt_fog', day: 0, img: '21.png', luxpercent: 0],   // NIGHT: Freezing fog - Light Freezing Fog
     [xucode: 1147, wuphrase: 'Patches of Fog', wucode: 'nt_fog', day: 0, img: '21.png', luxpercent: 0],   // NIGHT: Freezing fog - Patches of Fog
     [xucode: 1150, wuphrase: 'Light Drizzle', wucode: 'nt_rain', day: 0, img: '9.png', luxpercent: 0],   // NIGHT: Patchy light drizzle - Light Drizzle
     [xucode: 1153, wuphrase: 'Drizzle', wucode: 'nt_rain', day: 0, img: '9.png', luxpercent: 0],   // NIGHT: Light drizzle - Drizzle
     [xucode: 1153, wuphrase: 'Light Drizzle', wucode: 'nt_rain', day: 0, img: '9.png', luxpercent: 0],   // NIGHT: Light drizzle
     [xucode: 1153, wuphrase: 'Light Mist', wucode: 'nt_rain', day: 0, img: '9.png', luxpercent: 0],   // NIGHT: Light drizzle - Light Mist
     [xucode: 1153, wuphrase: 'Light Rain Mist', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Light drizzle - Light Rain Mist
     [xucode: 1153, wuphrase: 'Rain Mist', wucode: 'nt_rain', day: 0, img: '9.png', luxpercent: 0],   // NIGHT: Light drizzle - Rain Mist
     [xucode: 1168, wuphrase: 'Freezing Drizzle', wucode: 'nt_sleet', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Freezing drizzle
     [xucode: 1168, wuphrase: 'Light Freezing Drizzle', wucode: 'nt_sleet', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Freezing drizzle - Light Freezing Drizzle
     [xucode: 1171, wuphrase: 'Heavy Freezing Drizzle', wucode: 'nt_sleet', day: 0, img: '6.png', luxpercent: 0],   // NIGHT: Heavy freezing drizzle
     [xucode: 1180, wuphrase: 'Light Rain', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Patchy light rain - Light Rain
     [xucode: 1183, wuphrase: 'Heavy Mist', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Light rain - Heavy Mist
     [xucode: 1183, wuphrase: 'Heavy Rain Mist', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Light rain - Heavy Rain Mist
     [xucode: 1183, wuphrase: 'Light Rain', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Light rain
     [xucode: 1186, wuphrase: 'Rain', wucode: 'nt_rain', day: 0, img: '9.png', luxpercent: 0],   // NIGHT: Moderate rain at times - Rain
     [xucode: 1189, wuphrase: 'Heavy Drizzle', wucode: 'nt_rain', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Moderate rain - Heavy Drizzle
     [xucode: 1189, wuphrase: 'Rain', wucode: 'nt_rain', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Moderate rain - Rain
     [xucode: 1192, wuphrase: 'Heavy Rain', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Heavy rain at times - Heavy Rain
     [xucode: 1195, wuphrase: 'Heavy Rain', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Heavy rain
     [xucode: 1198, wuphrase: 'Light Freezing Rain', wucode: 'nt_sleet', day: 0, img: '6.png', luxpercent: 0],   // NIGHT: Light freezing rain
     [xucode: 1201, wuphrase: 'Heavy Freezing Rain', wucode: 'nt_rain', day: 0, img: '6.png', luxpercent: 0],   // NIGHT: Moderate or heavy freezing rain - Heavy Freezing Rain
     [xucode: 1204, wuphrase: 'Hail', wucode: 'nt_sleet', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Light sleet - Hail
     [xucode: 1204, wuphrase: 'Light Hail', wucode: 'nt_sleet', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Light sleet - Light Hail
     [xucode: 1204, wuphrase: 'Light Ice Crystals', wucode: 'nt_sleet', day: 0, img: '25.png', luxpercent: 0],   // NIGHT: Light sleet - Light Ice Crystals
     [xucode: 1204, wuphrase: 'Light Ice Pellets', wucode: 'nt_sleet', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Light sleet - Light Ice Pellets
     [xucode: 1204, wuphrase: 'Light Snow Grains', wucode: 'nt_sleet', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Light sleet - Light Snow Grains
     [xucode: 1204, wuphrase: 'Small Hail', wucode: 'nt_sleet', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Light sleet - Small Hail
     [xucode: 1207, wuphrase: 'Heavy Ice Crystals', wucode: 'nt_sleet', day: 0, img: '25.png', luxpercent: 0],   // NIGHT: Moderate or heavy sleet - Heavy Ice Crystals
     [xucode: 1210, wuphrase: 'Light Snow', wucode: 'nt_snow', day: 0, img: '13.png', luxpercent: 0],   // NIGHT: Patchy light snow - Light Snow
     [xucode: 1213, wuphrase: 'Light Snow', wucode: 'nt_snow', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Light snow
     [xucode: 1219, wuphrase: 'Snow', wucode: 'nt_snow', day: 0, img: '7.png', luxpercent: 0],   // NIGHT: Moderate snow - Snow
     [xucode: 1222, wuphrase: 'Heavy Snow', wucode: 'nt_snow', day: 0, img: '46.png', luxpercent: 0],   // NIGHT: Patchy heavy snow - Heavy Snow
     [xucode: 1225, wuphrase: 'Heavy Snow', wucode: 'snow', day: 0, img: '16.png', luxpercent: 0],   // NIGHT: Heavy snow
     [xucode: 1237, wuphrase: 'Ice Crystals', wucode: 'nt_sleet', day: 0, img: '16.png', luxpercent: 0],   // NIGHT: Ice pellets - Ice Crystals
     [xucode: 1237, wuphrase: 'Ice Pellets', wucode: 'nt_sleet', day: 0, img: '16.png', luxpercent: 0],   // NIGHT: Ice pellets
     [xucode: 1237, wuphrase: 'Snow Grains', wucode: 'nt_sleet', day: 0, img: '16.png', luxpercent: 0],   // NIGHT: Ice pellets - Snow Grains
     [xucode: 1240, wuphrase: 'Light Rain Showers', wucode: 'nt_rain', day: 0, img: '11.png', luxpercent: 0],   // NIGHT: Light rain shower - Light Rain Showers
     [xucode: 1243, wuphrase: 'Heavy Rain Showers', wucode: 'nt_rain', day: 0, img: '40.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain shower - Heavy Rain Showers
     [xucode: 1243, wuphrase: 'Rain Showers', wucode: 'nt_rain', day: 0, img: '40.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain shower - Rain Showers
     [xucode: 1246, wuphrase: 'Heavy Rain Showers', wucode: 'nt_rain', day: 0, img: '40.png', luxpercent: 0],   // NIGHT: Torrential rain shower - Heavy Rain Showers
     [xucode: 1249, wuphrase: 'Light Thunderstorms with Hail', wucode: 'nt_sleet', day: 0, img: '5.png', luxpercent: 0],   // NIGHT: Light sleet showers - Light Thunderstorms with Hail
     [xucode: 1252, wuphrase: 'Freezing Rain', wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy sleet showers - Freezing Rain
     [xucode: 1252, wuphrase: 'Heavy Small Hail Showers', wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy sleet showers - Heavy Small Hail Showers
     [xucode: 1252, wuphrase: 'Heavy Snow Grains', wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy sleet showers - Heavy Snow Grains
     [xucode: 1252, wuphrase: 'Ice Pellet Showers', wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy sleet showers - Ice Pellet Showers
     [xucode: 1252, wuphrase: 'Small Hail Showers', wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy sleet showers - Small Hail Showers
     [xucode: 1255, wuphrase: 'Light Snow Showers', wucode: 'nt_snow', day: 0, img: '16.png', luxpercent: 0],   // NIGHT: Light snow showers
     [xucode: 1258, wuphrase: 'Heavy Snow', wucode: 'nt_snow', day: 0, img: '42.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow showers - Heavy Snow
     [xucode: 1258, wuphrase: 'Heavy Snow Showers', wucode: 'nt_snow', day: 0, img: '42.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow showers - Heavy Snow Showers
     [xucode: 1258, wuphrase: 'Snow Blowing Snow Mist', wucode: 'nt_snow', day: 0, img: '41.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow showers - Snow Blowing Snow Mist
     [xucode: 1258, wuphrase: 'Snow Showers', wucode: 'nt_snow', day: 0, img: '41.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow showers - Snow Showers
     [xucode: 1261, wuphrase: 'Light Hail Showers', wucode: 'nt_snow', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Light showers of ice pellets - Light Hail Showers
     [xucode: 1261, wuphrase: 'Light Ice Pellet Showers', wucode: 'nt_snow', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Light showers of ice pellets - Light Ice Pellet Showers
     [xucode: 1261, wuphrase: 'Light Small Hail Showers', wucode: 'nt_snow', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Light showers of ice pellets - Light Small Hail Showers
     [xucode: 1261, wuphrase: 'Light Thunderstorms with Small Hail', wucode: 'nt_snow', day: 0, img: '8.png', luxpercent: 0],   // NIGHT: Light showers of ice pellets - Light Thunderstorms with Small Hail
     [xucode: 1264, wuphrase: 'Hail Showers', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Hail Showers
     [xucode: 1264, wuphrase: 'Heavy Hail', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Hail
     [xucode: 1264, wuphrase: 'Heavy Hail Showers', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Hail Showers
     [xucode: 1264, wuphrase: 'Heavy Ice Crystals', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Ice Crystals
     [xucode: 1264, wuphrase: 'Heavy Ice Pellet Showers', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Ice Pellet Showers
     [xucode: 1264, wuphrase: 'Heavy Ice Pellets', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Ice Pellets
     [xucode: 1264, wuphrase: 'Heavy Thunderstorms and Ice Pellets', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Thunderstorms and Ice Pellets
     [xucode: 1264, wuphrase: 'Heavy Thunderstorms with Hail', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Thunderstorms with Hail
     [xucode: 1264, wuphrase: 'Heavy Thunderstorms with Small Hail', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Heavy Thunderstorms with Small Hail
     [xucode: 1264, wuphrase: 'Thunderstorms with Small Hail', wucode: 'nt_sleet', day: 0, img: '3.png', luxpercent: 0],   // NIGHT: Moderate or heavy showers of ice pellets - Thunderstorms with Small Hail
     [xucode: 1273, wuphrase: 'Light Thunderstorm', wucode: 'nt_chancetstorms', day: 0, img: '47.png', luxpercent: 0],   // NIGHT: Patchy light rain with thunder - Light Thunderstorm
     [xucode: 1273, wuphrase: 'Light Thunderstorms and Rain', wucode: 'nt_chancetstorms', day: 0, img: '47.png', luxpercent: 0],   // NIGHT: Patchy light rain with thunder - Light Thunderstorms and Rain
     [xucode: 1276, wuphrase: 'Heavy Thunderstorm', wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain with thunder - Heavy Thunderstorm
     [xucode: 1276, wuphrase: 'Heavy Thunderstorms and Rain', wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain with thunder - Heavy Thunderstorms and Rain
     [xucode: 1276, wuphrase: 'Thunderstorm', wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain with thunder - Thunderstorm
     [xucode: 1276, wuphrase: 'Thunderstorms and Rain', wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain with thunder - Thunderstorms and Rain
     [xucode: 1276, wuphrase: 'Thunderstorms with Hail', wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0],   // NIGHT: Moderate or heavy rain with thunder - Thunderstorms with Hail
     [xucode: 1279, wuphrase: 'Light Thunderstorms and Ice Pellets', wucode: 'nt_chancesnow', day: 0, img: '41.png', luxpercent: 0],   // NIGHT: Patchy light snow with thunder - Light Thunderstorms and Ice Pellets
     [xucode: 1279, wuphrase: 'Light Thunderstorms and Snow', wucode: 'nt_chancesnow', day: 0, img: '41.png', luxpercent: 0],   // NIGHT: Patchy light snow with thunder - Light Thunderstorms and Snow
     [xucode: 1282, wuphrase: 'Heavy Thunderstorms and Snow', wucode: 'nt_snow', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow with thunder - Heavy Thunderstorms and Snow
     [xucode: 1282, wuphrase: 'Thunderstorms and Ice Pellets', wucode: 'nt_snow', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow with thunder - Thunderstorms and Ice Pellets
     [xucode: 1282, wuphrase: 'Thunderstorms and Snow', wucode: 'nt_snow', day: 0, img: '18.png', luxpercent: 0],   // NIGHT: Moderate or heavy snow with thunder - Thunderstorms and Snow
     [xucode: 1000, wuphrase: 'Breezy', wucode: 'nt_breezy', day: 0, img: '23.png', luxpercent: 0],   // NIGHT: Breezy
]    

@Field static attributesMap = [
	"alert":				    [title: "alert", descr: "Display any weather alert?", typeof: "string", default: "false"],
    "betwixt":				    [title: "Betwixt", descr: "Display the 'slice-of-day'?", typeof: "string", default: "false"],
    "chanceOfRain":		        [title: "chanceOfRain", descr: "Display the probability of precipitation (with '%')?", typeof: false, default: "false"],
	"city":				        [title: "City", descr: "Display your City's name?", typeof: "string", default: "true"],
	"cloud":			    	[title: "Cloud", descr: "Display cloud coverage %?", typeof: "number", default: "false"],
	"condition_code":			[title: "Condition code", descr: "Display 'condition_code'?", typeof: "string", default: "false"],
	"condition_icon_only":		[title: "Condition icon only", descr: "Display 'condition_code_only'?", typeof: "string", default: "false"],
	"condition_icon_url":		[title: "Condition icon URL", descr: "Display 'condition_code_url'?", typeof: "string", default: "false"],
	"condition_icon":			[title: "Condition icon", descr: "Dislay 'condition_icon'?", typeof: "string", default: "false"],
    "condition_iconWithText":   [title: "Condition icon with text", descr: "Display 'condition_iconWithText'?", typeof: "string", default: "false"],    
	"condition_text":			[title: "Condition text", descr: "Display 'condition_text'?", typeof: "string", default: "false"],
	"country":				    [title: "Country", descr: "Display 'country'?", typeof: "string", default: "false"],
    "dewpoint":                 [title: "dewpoint (in default unit)", descr: "Display the dewpoint?", typeof: "number", default: "true"],
    "fcstHighLow":              [title: "Forecast High/Low Temperatures:", descr: "Display forecast High/Low temperatures?", typeof: false, default: "false"],
    "feelsLike":			    [title: "Feels like (in default unit)", descr: "Display the 'feels like' temperature?", typeof: "number", default: "true"],
	"forecastIcon":			    [title: "Forecast icon", descr: "Display an Icon of the Forecast Weather?", typeof: "string", default: "true"],
	"forecast_code":		    [title: "Forecast code", descr: "Display 'forecast_code'?", typeof: "string", default: "false"],
	"forecast_text":		    [title: "Forecast text", descr: "Display 'forecast_text'?", typeof: "string", default: "false"],
	"illuminance":			    [title: "Illuminance", descr: "Display 'illuminance'?", typeof: "number", default: "true"],    
	"illuminated":			    [title: "Illuminated", descr: "Display 'illuminated' (with 'lux' added for use on a Dashboard)?", typeof: "string", default: "true"],
	"is_day":				    [title: "Is daytime", descr: "Display 'is_day'?", typeof: "number", default: "false"],
	"lat":				        [title: "Latitude and Longitude", descr: "Display both Latitude and Longitude?", typeof: false, default: "false"],
	"localSunrise":			    [title: "Local SunRise and SunSet", descr: "Display the Group of 'Time of Local Sunrise and Sunset,' with and without Dashboard text?", typeof: false, default: "true"],
	"location":				    [title: "Location name with region", descr: "", typeof: "string", default: "false"],
	"mytile":				    [title: "Mytile for dashboard", descr: "Display 'mytile'?", typeof: "string", default: "false"],
	"moonPhase":			    [title: "Moon Phase", descr: "Display 'moonPhase'?", typeof: "string", default: "false"],    
	"percentPrecip":			[title: "Percent precipitation", descr: "Display the Chance of Rain, in percent?", typeof: "number", default: "true"],
    "solarradiation":			[title: "Solar Radiation", descr: "Display 'solarradiation'?", typeof: "string", default: "false"],
    "summarymessage":			[title: "Summary Message", descr: "Display the Weather Summary?", typeof: false, default: "false"],
	"precipExtended":			[title: "Extended Precipitation", descr: "Display precipitation forecast?", typeof: false, default: "false"],
    "obspoll":			        [title: "Observation time", descr: "Display Observation and Poll times?", typeof: false, default: "false"], 
    "precip_today":			    [title: "Precipitation today (in default unit)", descr: "Display precipitation today?", typeof: "number", default: "false"],
	"region":				    [title: "Region", descr: "Display 'region'?", typeof: "string", default: "false"],
	"state":				    [title: "State", descr: "Display 'state'?", typeof: "string", default: "false"],    
    "UV":                       [title: "Ultraviolet index", descr: "Display ultraviolet index?", typeof: "string", default: "false"],
	"vis":				        [title: "Visibility (in default unit)", descr: "Display visibility distance?", typeof: "number", default: "false"],
	"weather":				    [title: "Weather", descr: "Display Current Conditions?", typeof: "string", default: "false"],
	"wind":				        [title: "Wind (in default unit)", descr: "Display the Wind Speed?", typeof: "number", default: "true"],
	"wind_degree":			    [title: "Wind Degree", descr: "Display the Wind Direction (number)?", typeof: "number", default: "false"],
	"wind_direction":			[title: "Wind direction", descr: "Display the Wind Direction?", typeof: "string", default: "true"],
	"wind_gust":				[title: "Wind gust (in default unit)", descr: "Display the Wind Gust?", typeof: "number", default: "true"],
	"wind_string":			    [title: "Wind string", descr: "Display the wind string?", typeof: "string", default: "false"],
]
