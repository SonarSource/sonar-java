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
//                            ^^^> {{Logger already declared here.}}
  private static final Logger LOGGER = LoggerFactory.getLogger(MoreThanOneLoggerCheckSample.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                            ^^^^^^

  void log() {
    LOG.info("one");
    LOGGER.info("two");
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
//                            ^^^^^> {{Logger already declared here.}}
  private static final Logger SECOND = LoggerFactory.getLogger(ThreeLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                            ^^^^^^
  private static final Logger THIRD = LoggerFactory.getLogger(ThreeLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                            ^^^^^
//                            ^^^^^@-5< {{Logger already declared here.}}
}

class MixedFacades {

  private static final Logger SLF4J = LoggerFactory.getLogger(MixedFacades.class);
//                            ^^^^^> {{Logger already declared here.}}
  private static final org.apache.logging.log4j.Logger LOG4J2 = LogManager.getLogger(MixedFacades.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                                                     ^^^^^^
}

class JulLoggers {

  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("first");
//                                              ^^^> {{Logger already declared here.}}
  private static final java.util.logging.Logger OTHER = java.util.logging.Logger.getLogger("first"); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                                              ^^^^^
}

class CommonsLoggingLoggers {

  private static final Log LOG = LogFactory.getLog(CommonsLoggingLoggers.class);
//                         ^^^> {{Logger already declared here.}}
  private static final Log OTHER = LogFactory.getLog(CommonsLoggingLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                         ^^^^^
}

class NonStaticNextToStatic {

  private static final Logger LOG = LoggerFactory.getLogger(NonStaticNextToStatic.class);
//                            ^^^> {{Logger already declared here.}}
  private final Logger instanceLogger = LoggerFactory.getLogger(NonStaticNextToStatic.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                     ^^^^^^^^^^^^^^
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
//                              ^^^^^> {{Logger already declared here.}}
    private static final Logger SECOND = LoggerFactory.getLogger(Nested.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                              ^^^^^^
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
//                            ^^^> {{Logger already declared here.}}
  private static final Logger OTHER = LoggerFactory.getLogger(EnumWithTwoLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                            ^^^^^
}

record RecordWithTwoLoggers(String name) {

  private static final Logger LOG = LoggerFactory.getLogger(RecordWithTwoLoggers.class);
//                            ^^^> {{Logger already declared here.}}
  private static final Logger OTHER = LoggerFactory.getLogger(RecordWithTwoLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                            ^^^^^
}

interface InterfaceWithTwoLoggers {

  Logger LOG = LoggerFactory.getLogger(InterfaceWithTwoLoggers.class);
//       ^^^> {{Logger already declared here.}}
  Logger OTHER = LoggerFactory.getLogger(InterfaceWithTwoLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//       ^^^^^
}

class ConfigurableLoggerProperty {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurableLoggerProperty.class); // Compliant
  private Logger contextLogger; // Compliant, not initialized here, it is a configurable property

  Logger getContextLogger() {
    return contextLogger;
  }

  void setContextLogger(Logger logger) {
    contextLogger = logger;
  }

  void start() {
    if (contextLogger == null) {
      contextLogger = LoggerFactory.getLogger(ConfigurableLoggerProperty.class.getName() + ".context");
    }
    LOG.info("started");
  }
}

class DerivedChildLogger {

  private static final Logger LOG = LoggerFactory.getLogger(DerivedChildLogger.class); // Compliant
  private static final Logger LOG_CONFIG = LoggerFactory.getLogger(LOG.getName() + ".config"); // Compliant, a child category inherits the parent configuration

  void log() {
    LOG.info("one");
    LOG_CONFIG.debug("configuration detail");
  }
}

@interface AnnotationTypeWithTwoLoggers {

  Logger LOG = LoggerFactory.getLogger(AnnotationTypeWithTwoLoggers.class);
//       ^^^> {{Logger already declared here.}}
  Logger OTHER = LoggerFactory.getLogger(AnnotationTypeWithTwoLoggers.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//       ^^^^^
}

class MultipleLoggerFieldsInOneDeclaration {

  private static final Logger FIRST = LoggerFactory.getLogger(MultipleLoggerFieldsInOneDeclaration.class), SECOND = LoggerFactory.getLogger(MultipleLoggerFieldsInOneDeclaration.class); // Noncompliant {{Remove this logger and use the one already declared in this class.}}
//                            ^^^^^> {{Logger already declared here.}}                                     ^^^^^^
}
