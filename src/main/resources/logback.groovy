import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.util.Duration

import static ch.qos.logback.classic.Level.*

if (System.getProperty("log.debug") != null) {
	println "Logback configuration debugging enabled"
	
	statusListener(OnConsoleStatusListener)
}

def LOG_LEVEL = toLevel(System.getProperty("log.level"), INFO)

def haveBeagle = System.getProperty("log.beagle") != null 
def logOps = System.getProperty("log.ops") != null  

appender("CONSOLE", ConsoleAppender) {
	
	filter(ThresholdFilter) {
	  level = toLevel(System.getProperty("log.level"), DEBUG)
	}
	
	encoder(PatternLayoutEncoder) {
	  pattern = "%d{HH:mm:ss.SSS} [%thread %file:%line] %-5level %logger{0} - %msg%n"
	}
}

def appenders = [ "CONSOLE" ]

if (haveBeagle) {
	appender("SOCKET", SocketAppender) {
	  includeCallerData = true
	  remoteHost = "localhost"
	  port = 4321
	  reconnectionDelay = new Duration(10000)
	}
	
	appenders += ["SOCKET"]
}

root(LOG_LEVEL, appenders)

if(logOps && !(LOG_LEVEL in [ TRACE, DEBUG ])) {
	logger("VMRunner", DEBUG)
}

