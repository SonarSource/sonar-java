package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class A {

  Logger logger1 = LoggerFactory.getLogger(Wrong.class); // Noncompliant {{Update this logger to use "A.class".}}
  Logger logger2 = LoggerFactory.getLogger(Wrong.class.getName()); // Noncompliant {{Update this logger to use "A.class".}}
  Logger logger3 = LoggerFactory.getLogger(A.class);
  Logger logger4 = LoggerFactory.getLogger(A.class.getName());
  Logger logger5 = LoggerFactory.getLogger("INFO");
  Logger loggerNoInit;
  Logger loggerNotLiteral = LoggerFactory.getLogger(A.s);
  Logger loggerNotLiteral2 = LoggerFactory.getLogger(this.foo());

  Log apache1 = LogFactory.getLog(Wrong.class); // Noncompliant
  Log apache2 = LogFactory.getLog(A.class);

  java.util.logging.Logger jul1 = java.util.logging.Logger.getLogger(Wrong.class.getName()); // Noncompliant
  java.util.logging.Logger jul2 = java.util.logging.Logger.getLogger(A.class.getName());

  org.apache.logging.log4j.Logger log4jA = org.apache.logging.log4j.LogManager.getLogger(Wrong.class); // Noncompliant
  org.apache.logging.log4j.Logger log4jB = org.apache.logging.log4j.LogManager.getLogger(Wrong.class.getName()); // Noncompliant

  org.sonar.api.utils.log.Logger sonar = org.sonar.api.utils.log.Loggers.get(Wrong.class); // Noncompliant

  int f = 3;
  String s = foo();

  public String foo() {

  }

}

class Wrong {

}
