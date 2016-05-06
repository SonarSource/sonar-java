import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

class A {
	final static String MSG1 = "ok";
	final static Logger logger = LoggerFactory.getLogger("A");

	private void method() {
		String notok = "not ok";
		logger.debug("check OK");
		logger.debug(MSG1);
		logger.warn("check" + "NOT OK");
		logger.warn("check" + notok);  // Noncompliant {{Avoid Object concatenating in log messages.}}
		logger.error(String.format("This is %s OK", "NOT"));  // Noncompliant {{String.format() should not be used as a log message.}}
		logger.error(logmsg());  // Noncompliant {{A method call should not be used as a log messages.}}
		logger.error("This is " + "NOT" + " OK");
		logger.error("Is this OK" + false);  // Noncompliant {{Avoid Object concatenating in log messages.}}
		logger.info("this is ok {}", true);
		logger.info(MarkerFactory.getIMarkerFactory().getMarker("test"), "msg");
		logger.info(MarkerFactory.getIMarkerFactory().getMarker("test"), "msg " + false);  // Noncompliant {{Avoid Object concatenating in log messages.}}
	}
	
	private String logmsg(){ return "dummy"; }
}
