package checks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

class MoreThanOneLoggerCheckSample {

  private static final Logger LOG = LoggerFactory.getLogger(MoreThanOneLoggerCheckSample.class);
  private static final Logger AUDIT = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]

  void log() {
    LOG.info("one");
    AUDIT.info("two");
  }
}

class SingleLogger {

  private static final Logger LOG = LoggerFactory.getLogger(SingleLogger.class); // Compliant

  void log() {
    LOG.info("one");
  }
}

class NoLogger {

  private static final String NAME = "no logger here"; // Compliant

  private final Object delegate = new Object(); // Compliant
}

class ThreeLoggers {

  private static final Logger FIRST = LoggerFactory.getLogger(ThreeLoggers.class);
  private static final Logger SECOND = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
  private static final Logger THIRD = LoggerFactory.getLogger(String.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-2]]
}

class MixedFacades {

  private static final Logger SLF4J = LoggerFactory.getLogger(MixedFacades.class);
  private static final org.apache.logging.log4j.Logger LOG4J2 = LogManager.getLogger(MixedFacades.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

class JulLoggers {

  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("first");
  private static final java.util.logging.Logger OTHER = java.util.logging.Logger.getLogger("second"); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

class CommonsLoggingLoggers {

  private static final Log LOG = LogFactory.getLog(CommonsLoggingLoggers.class);
  private static final Log OTHER = LogFactory.getLog(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

class NonStaticNextToStatic {

  private static final Logger LOG = LoggerFactory.getLogger(NonStaticNextToStatic.class);
  private final Logger instanceLogger = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

class LoggerWithMarker {

  private static final Logger LOG = LoggerFactory.getLogger(LoggerWithMarker.class); // Compliant
  private static final Marker AUDIT = MarkerFactory.getMarker("AUDIT"); // Compliant, a marker is not a logger

  void log() {
    LOG.info(AUDIT, "audited");
  }
}

class OuterWithNestedClasses {

  private static final Logger LOG = LoggerFactory.getLogger(OuterWithNestedClasses.class); // Compliant

  static class Nested {
    private static final Logger NESTED_LOG = LoggerFactory.getLogger(Nested.class); // Compliant, a nested class is a class of its own
  }

  class Inner {
    private final Logger innerLog = LoggerFactory.getLogger(Inner.class); // Compliant, an inner class is a class of its own
  }
}

class NestedClassWithTwoLoggers {

  private static final Logger LOG = LoggerFactory.getLogger(NestedClassWithTwoLoggers.class); // Compliant

  static class Nested {
    private static final Logger FIRST = LoggerFactory.getLogger(Nested.class);
    private static final Logger SECOND = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
  }
}

class AnonymousClassLogger {

  private static final Logger LOG = LoggerFactory.getLogger(AnonymousClassLogger.class); // Compliant

  Runnable task() {
    return new Runnable() {
      private final Logger anonymousLog = LoggerFactory.getLogger(Runnable.class); // Compliant, the anonymous class body is its own class

      @Override
      public void run() {
        anonymousLog.info("run");
      }
    };
  }
}

class LocalLoggerVariables {

  private static final Logger LOG = LoggerFactory.getLogger(LocalLoggerVariables.class); // Compliant

  void log() {
    Logger local = LoggerFactory.getLogger(Object.class); // Compliant, a local variable is not a field
    Logger otherLocal = LoggerFactory.getLogger(String.class); // Compliant
    local.info("one");
    otherLocal.info("two");
    LOG.info("three");
  }
}

enum EnumWithTwoLoggers {
  INSTANCE;

  private static final Logger LOG = LoggerFactory.getLogger(EnumWithTwoLoggers.class);
  private static final Logger OTHER = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

record RecordWithTwoLoggers(String name) {

  private static final Logger LOG = LoggerFactory.getLogger(RecordWithTwoLoggers.class);
  private static final Logger OTHER = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

interface InterfaceWithTwoLoggers {

  Logger LOG = LoggerFactory.getLogger(InterfaceWithTwoLoggers.class);
  Logger OTHER = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}} [[secondary=-1]]
}

class MultipleLoggerFieldsInOneDeclaration {

  private static final Logger FIRST = LoggerFactory.getLogger(MultipleLoggerFieldsInOneDeclaration.class), SECOND = LoggerFactory.getLogger(Object.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
}
