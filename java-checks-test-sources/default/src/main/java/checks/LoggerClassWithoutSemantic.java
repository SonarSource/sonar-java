package checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class LoggerClassWithoutSemantic {

  Logger logger1 = LoggerFactory.getLogger(Wrong.class); // FN
  Logger logger2 = LoggerFactory.getLogger(Wrong.class.getName()); // FN
  Logger logger3 = LoggerFactory.getLogger(LoggerClass.class);
  Logger logger4 = LoggerFactory.getLogger(LoggerClass.class.getName());
  Logger logger5 = LoggerFactory.getLogger("INFO");
  Logger loggerNoInit;
  Logger loggerNotLiteral = LoggerFactory.getLogger(LoggerClass.s);
  Logger loggerNotLiteral2 = LoggerFactory.getLogger(this.foo());

  Log apache1 = LogFactory.getLog(Wrong.class); // FN
  Log apache2 = LogFactory.getLog(LoggerClass.class);

  java.util.logging.Logger jul1 = java.util.logging.Logger.getLogger(Wrong.class.getName()); // Noncompliant
  java.util.logging.Logger jul2 = java.util.logging.Logger.getLogger(LoggerClass.class.getName()); // Noncompliant

  org.apache.logging.log4j.Logger log4jA = org.apache.logging.log4j.LogManager.getLogger(Wrong.class); // FN
  org.apache.logging.log4j.Logger log4jB = org.apache.logging.log4j.LogManager.getLogger(Wrong.class.getName()); // FN

  org.sonar.api.utils.log.Logger sonar = org.sonar.api.utils.log.Loggers.get(Wrong.class); // FN

  int f = 3;
  static String s = foo();

  public static String foo() { return ""; }

  class Generic<K, V> {
    Logger logger = LoggerFactory.getLogger(Generic.class.getName()); // Compliant
  }

  class Wrong { }
}
