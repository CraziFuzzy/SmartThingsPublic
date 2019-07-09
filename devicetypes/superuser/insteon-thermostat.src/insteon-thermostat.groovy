/**
 *  Insteon Thermostat
 *
 *  Derived from work on Insteon Dimmer Switch & Plug by:
 *
 *  Original Author     : ethomasii@gmail.com
 *  Creation Date       : 2013-12-08
 *
 *  Rewritten by        : idealerror
 *  Last Modified Date  : 2016-12-13 
 *
 *  Rewritten by        : kuestess
 *  Last Modified Date  : 2017-12-30
 *  
 *  Changelog:
 * 
 *  2019-07-08: Started converting to thermostat device
 *  2017-12-30: Corrected getStatus command2 to be 00 [jens@ratsey.com]
 *  2016-12-13: Added polling for Hub2
 *  2016-12-13: Added background refreshing every 3 minutes
 *  2016-11-21: Added refresh/polling functionality
 *  2016-10-15: Added full dimming functions
 *  2016-10-01: Redesigned interface tiles
 */
 
import groovy.json.JsonSlurper
 
preferences {
    input("deviceid", "text", title: "Device ID", description: "Your Insteon device.  Do not include periods example: FF1122.")
    input("host", "text", title: "URL", description: "The URL of your Hub (without http:// example: my.hub.com ")
    input("port", "text", title: "Port", description: "The hub port.")
    input("username", "text", title: "Username", description: "The hub username (found in app)")
    input("password", "text", title: "Password", description: "The hub password (found in app)")
} 
 
metadata {
    definition (name: "Insteon Thermostat", author: "CraziFuzzy", oauth: true) {
        capability "Actuator"
        capability "Thermostat"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Health Check"
        capability "Polling"
        capability "Refresh"

        command "tempUp"
        command "tempDown"
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "setpointUp"
        command "setpointDown"

        command "cycleMode"
        command "cycleFanMode"

    }

    // simulator metadata
    simulator {
    }

    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("temp", label:'${currentValue}°', unit:"°F", defaultState: true)
            }
            tileAttribute("device.temperature", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "setpointUp")
                attributeState("VALUE_DOWN", action: "setpointDown")
            }
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
                attributeState("humidity", label: '${currentValue}%', unit: "%", defaultState: true)
            }
            tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
                attributeState("idle", backgroundColor: "#FFFFFF")
                attributeState("heating", backgroundColor: "#E86D13")
                attributeState("cooling", backgroundColor: "#00A0DC")
            }
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
                attributeState("off",  label: '${name}')
                attributeState("heat", label: '${name}')
                attributeState("cool", label: '${name}')
                attributeState("auto", label: '${name}')
                attributeState("emergency heat", label: 'e-heat')
            }
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "°F", defaultState: true)
            }
            tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "°F", defaultState: true)
            }
        }

        standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off",            action: "cycleMode", nextState: "updating", icon: "st.thermostat.heating-cooling-off", backgroundColor: "#CCCCCC", defaultState: true
            state "heat",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.heat"
            state "cool",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.cool"
            state "auto",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.auto"
            state "emergency heat", action: "cycleMode", nextState: "updating", icon: "st.thermostat.emergency-heat"
            state "updating", label: "Working"
        }

        standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
            state "off",       action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-off", backgroundColor: "#CCCCCC", defaultState: true
            state "auto",      action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-auto"
            state "on",        action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-on"
            state "circulate", action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-circulate"
            state "updating", label: "Working"
        }

        valueTile("heatingSetpoint", "device.heatingSetpoint", width: 2, height: 2, decoration: "flat") {
            state "heat", label:'Heat\n${currentValue}°', unit: "°F", backgroundColor:"#E86D13"
        }
        standardTile("heatDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "heat", action: "heatDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("heatUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "heat", action: "heatUp", icon: "st.thermostat.thermostat-up"
        }

        valueTile("coolingSetpoint", "device.coolingSetpoint", width: 2, height: 2, decoration: "flat") {
            state "cool", label: 'Cool\n${currentValue}°', unit: "°F", backgroundColor: "#00A0DC"
        }
        standardTile("coolDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "cool", action: "coolDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("coolUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "cool", action: "coolUp", icon: "st.thermostat.thermostat-up"
        }

        valueTile("roomTemp", "device.temperature", width: 2, height: 1, decoration: "flat") {
            state "default", label:'${currentValue}°', unit: "°F", backgroundColors: [
                // Celsius Color Range
                [value:  0, color: "#153591"],
                [value:  7, color: "#1E9CBB"],
                [value: 15, color: "#90D2A7"],
                [value: 23, color: "#44B621"],
                [value: 29, color: "#F1D801"],
                [value: 33, color: "#D04E00"],
                [value: 36, color: "#BC2323"],
                // Fahrenheit Color Range
                [value: 40, color: "#153591"],
                [value: 44, color: "#1E9CBB"],
                [value: 59, color: "#90D2A7"],
                [value: 74, color: "#44B621"],
                [value: 84, color: "#F1D801"],
                [value: 92, color: "#D04E00"],
                [value: 96, color: "#BC2323"]
            ]
        }
        standardTile("tempDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "temp", action: "tempDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("tempUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "temp", action: "tempUp", icon: "st.thermostat.thermostat-up"
        }

        main("roomTemp")
        details(["thermostatMulti",
            "heatDown", "heatUp",
            "mode",
            "coolDown", "coolUp",
            "heatingSetpoint",
            "coolingSetpoint",
            "fanMode",
            "blank2x1", "blank2x1",
            "deviceHealthControl", "refresh", "reset",
            "roomTemp"
        ])
    }
}

// Not in use
def parse(String description) {
}

def on() {
    log.debug "Turning device ON"
    sendCmd("11", "FF")
    sendEvent(name: "switch", value: "on");
    sendEvent(name: "level", value: 100, unit: "%")
}

def off() {
    log.debug "Turning device OFF"
    sendCmd("13", "00")
    sendEvent(name: "switch", value: "off");
    sendEvent(name: "level", value: 0, unit: "%")
}

def setLevel(value) {

    // log.debug "setLevel >> value: $value"
    
    // Max is 255
    def percent = value / 100
    def realval = percent * 255
    def valueaux = realval as Integer
    def level = Math.max(Math.min(valueaux, 255), 0)
    if (level > 0) {
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }
    // log.debug "dimming value is $valueaux"
    log.debug "dimming to $level"
    dim(level,value)
}

def dim(level, real) {
    String hexlevel = level.toString().format( '%02x', level.toInteger() )
    // log.debug "Dimming to hex $hexlevel"
    sendCmd("11",hexlevel)
    sendEvent(name: "level", value: real, unit: "%")
}

def sendCmd(num, level)
{
    log.debug "Sending Command"

    // Will re-test this later
    // sendHubCommand(new physicalgraph.device.HubAction("""GET /3?0262${settings.deviceid}0F${num}${level}=I=3 HTTP/1.1\r\nHOST: IP:PORT\r\nAuthorization: Basic B64STRING\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
    httpGet("http://${settings.username}:${settings.password}@${settings.host}:${settings.port}//3?0262${settings.deviceid}0F${num}${level}=I=3") {response -> 
        def content = response.data
        
        // log.debug content
    }
    log.debug "Command Completed"
}

def refresh()
{
    log.debug "Refreshing.."
    poll()
}

def poll()
{
    log.debug "Polling.."
    getStatus()
    runIn(180, refresh)
}

def ping()
{
    log.debug "Pinging.."
    poll()
}

def initialize(){
    poll()
}

def getStatus() {

    def myURL = [
    	uri: "http://${settings.username}:${settings.password}@${settings.host}:${settings.port}/3?0262${settings.deviceid}0F1900=I=3"
    ]
    
    log.debug myURL
    httpPost(myURL)
	
    def buffer_status = runIn(2, getBufferStatus)
}

def getBufferStatus() {
	def buffer = ""
	def params = [
        uri: "http://${settings.username}:${settings.password}@${settings.host}:${settings.port}/buffstatus.xml"
    ]
    
    try {
        httpPost(params) {resp ->
            buffer = "${resp.responseData}"
            log.debug "Buffer: ${resp.responseData}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }

	def buffer_end = buffer.substring(buffer.length()-2,buffer.length())
	def buffer_end_int = Integer.parseInt(buffer_end, 16)
    
    def parsed_buffer = buffer.substring(0,buffer_end_int)
    log.debug "ParsedBuffer: ${parsed_buffer}"
    
    def responseID = parsed_buffer.substring(22,28)
    
    if (responseID == settings.deviceid) {
        log.debug "Response is for correct device: ${responseID}"
        def status = parsed_buffer.substring(38,40)
        log.debug "Status: ${status}"
		
        def level = Math.round(Integer.parseInt(status, 16)*(100/255))
        log.debug "Level: ${level}"
        
        if (level == 0) {
            log.debug "Device is off..."
            sendEvent(name: "switch", value: "off")
            sendEvent(name: "level", value: level, unit: "%")
            }

        else if (level > 0) {
            log.debug "Device is on..."
            sendEvent(name: "switch", value: "on")
            sendEvent(name: "level", value: level, unit: "%")
        }
    } else {
    	log.debug "Response is for wrong device - trying again"
        getStatus()
    }
}