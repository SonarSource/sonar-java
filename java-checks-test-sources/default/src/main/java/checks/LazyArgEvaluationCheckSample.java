package checks;

import com.google.common.base.Preconditions;
import java.util.Date;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("java:S139")
class LazyArgEvaluationCheckSample {

  public static final Logger slf4j = LoggerFactory.getLogger(LazyArgEvaluationCheckSample.class);
  public static final java.util.logging.Logger logger = java.util.logging.Logger.getGlobal();
  public static final org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager.getLogger();

  public static void recordFieldsAccessors() {
    record Person(String name, String age) {
      String age(String age) {
        return age;
      }
    }

    Person person = new Person("John", "42");
    logger.log(Level.SEVERE, "Something went wrong: " + person.name()); // false-negative getter has not the same logic than identifier
    logger.log(Level.SEVERE, person.name()); // Compliant - getters are OK
    logger.log(Level.SEVERE, person.age()); // Compliant - getters are OK
    logger.log(Level.SEVERE, person.age("12")); // Noncompliant - not a getter
  }

  public static void main(String[] args) {
    String message = "";

    logger.log(Level.SEVERE, message); // Compliant

    logger.log(Level.SEVERE, "Something went wrong: " + message);  // Noncompliant {{Use the built-in formatting to construct this argument.}}

    logger.log(Level.SEVERE, () -> "Something went wrong: " + message); // since Java 8, we can use Supplier , which will be evaluated lazily

    checkState(System.currentTimeMillis() == new Date().getTime(), "Arg must be positive, but got " + System.currentTimeMillis());  // Noncompliant {{Invoke method(s) only conditionally. Use the built-in formatting to construct this argument.}}

    Preconditions.checkState(System.currentTimeMillis() > 0, formatMessage());  // Noncompliant {{Invoke method(s) only conditionally. }}

    checkState(System.currentTimeMillis() > 0, "message: %s", formatMessage());  // Noncompliant {{Invoke method(s) only conditionally. }}

    checkState(System.currentTimeMillis() > 0, "message: %s", LazyArgEvaluationCheckSample.formatMessage());  // Noncompliant {{Invoke method(s) only conditionally. }}
  }

  public static void cachingOnDisk(File path) {
    logger.severe("Caching on disk @ " + path.getAbsolutePath()); // false-negative getter has not the same logic than identifier
    logger.severe("Caching on disk @ " + path.isAbsolute()); // false-negative getter has not the same logic than identifier
    slf4j.info("Caching on disk @ {}", path.getAbsolutePath()); // Compliant - getters are OK
    slf4j.info("Caching on disk @ {}", path.isAbsolute()); // Compliant - getters are OK
  }

  public void exceptionalPaths(File path) {
    try {

    } catch (Exception e) {
      slf4j.info("Caching on disk @ {}", path.getAbsolutePath()); // Compliant - because we don't care about small performance loss in exceptional paths
      new Runnable() {
        @Override
        public void run() {
          slf4j.info("logging all the time consuming resources for nothing " + computeValue()); // Noncompliant
        }

        String computeValue() { return ""; }
      };
    }
  }

  public void multiArgs() {
    checkState(System.currentTimeMillis() > 0, "message: %s %s", formatMessage(), "Something went wrong: " + System.currentTimeMillis());  // Noncompliant
  }

  private static String formatMessage() {
    return "Expensive computation";
  }

  public void classTree() {
    logger.log(Level.SEVERE, "Something went wrong: " + new Object() { // Noncompliant {{Invoke method(s) only conditionally. Use the built-in formatting to construct this argument.}}
      @Override
      public String toString() {
        return "tostring";
      }
    });
  }

  void slf4j(String csvPath, boolean condition) {
    slf4j.trace("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    slf4j.debug("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    slf4j.info("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    slf4j.warn("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    slf4j.error("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant {{Use the built-in formatting to construct this argument.}}

    slf4j.error("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant
    slf4j.error("Unable to open file " + csvPath, new RuntimeException(), 1);  // Noncompliant
    slf4j.error("Unable to open file " + csvPath, new RuntimeException(), 1, 2);  // Noncompliant
    slf4j.error("Unable to open file " + csvPath, new RuntimeException(), 1, "vargs", "vargs", "vargs FTW!");  // Noncompliant

    Marker confidentialMarker = MarkerFactory.getMarker("CONFIDENTIAL");
    slf4j.error(confidentialMarker, "Unable to open file " + csvPath, new RuntimeException());  // Noncompliant
    slf4j.error(confidentialMarker, "Unable to open file " + csvPath, new RuntimeException(), 1);  // Noncompliant
    slf4j.error(confidentialMarker, "Unable to open file " + csvPath, new RuntimeException(), 1, 2);  // Noncompliant
    slf4j.error(confidentialMarker, "Unable to open file " + csvPath, new RuntimeException(), 1, "vargs", "vargs", "vargs FTW!");  // Noncompliant

    if (slf4j.isTraceEnabled()) {
      slf4j.trace("Unable to open file " + csvPath, new RuntimeException());  // Compliant - inside if test
    }
    if (slf4j.isDebugEnabled()) {
      slf4j.debug("Unable to open file " + csvPath, new RuntimeException());  // Compliant - inside if test
    }
    if (slf4j.isInfoEnabled()) {
      slf4j.info("Unable to open file " + csvPath, new RuntimeException());  // Compliant - inside if test
    }
    if (slf4j.isWarnEnabled()) {
      slf4j.warn("Unable to open file " + csvPath, new RuntimeException());  // Compliant - inside if test
    }
    if (slf4j.isErrorEnabled()) {
      slf4j.error("Unable to open file " + csvPath, new RuntimeException());  // Compliant - inside if test
    }
    if (condition) {
      slf4j.error("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant
    }
  }

  void jul(String csvPath) {
    logger.finest("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    logger.finer("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    logger.fine("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    logger.config("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    logger.info("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    logger.warning("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}
    logger.severe("Unable to open file " + csvPath);  // Noncompliant {{Use the built-in formatting to construct this argument.}}

    if (logger.isLoggable(Level.FINEST)) {
      logger.finest("Unable to open file " + csvPath);  // Compliant - inside if test
    }

    if (logger.isLoggable(Level.INFO)) {
      logger.finer("Unable to open file " + csvPath);  // Compliant - FN, we don't verify that level in "if" matches actual level used in logging
      logger.info("Unable to open file " + csvPath);  // Compliant
    }
  }

  void log4j(String csvPath, org.apache.logging.log4j.Marker marker) {
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "Unable to open file " + csvPath); // Noncompliant {{Use the built-in formatting to construct this argument.}}
    log4j.debug("Unable to open file " + csvPath); // Noncompliant
    log4j.error("Unable to open file " + csvPath); // Noncompliant
    log4j.fatal("Unable to open file " + csvPath); // Noncompliant
    log4j.info("Unable to open file " + csvPath); // Noncompliant
    log4j.trace("Unable to open file " + csvPath); // Noncompliant
    log4j.warn("Unable to open file " + csvPath); // Noncompliant

    if (log4j.isDebugEnabled()) {
      log4j.debug("Unable to open file " + csvPath);
    }
    if (log4j.isEnabled(org.apache.logging.log4j.Level.DEBUG)) {
      log4j.debug("Unable to open file " + csvPath);
    }
    if (log4j.isEnabled(org.apache.logging.log4j.Level.DEBUG, marker)) {
      log4j.debug("Unable to open file " + csvPath);
    }
    log4j.debug(() -> "hello"); // using supplier
    log4j.debug(() -> new org.apache.logging.log4j.message.StringFormatterMessageFactory().newMessage("Unable to open file " + csvPath));
    log4j.debug("Unable to open file {0}", csvPath);
  }

}

class AddTwoArguments {
  private static final Logger LOGGER = LoggerFactory.getLogger(AddTwoArguments.class);
  private static final XLogger X_LOGGER = XLoggerFactory.getXLogger(AddTwoArguments.class);

  void foo(int timeout, String units) {
    LOGGER.debug("Setting read timeout to " + timeout + " " + units); // Noncompliant
    X_LOGGER.debug("Setting read timeout to " + timeout + " " + units); // Noncompliant
  }
}

class ConstantInlining {
  Logger logger = LoggerFactory.getLogger(ConstantInlining.class);

  static final String MY_CONST = "world";
  final String myField = "world";
  static String myStaticField = "world";

  void foo(boolean answer) {
    logger.warn("hello " + MY_CONST + ". Is this inlined by the compiler? {}", answer);
    logger.warn("hello " + ConstantInlining.MY_CONST + ". Is this inlined by the compiler? {}", answer);
    logger.warn("hello " + myField + ". Is this inlined by the compiler? {}", answer); // Noncompliant
    logger.warn("hello " + myStaticField + ". Is this inlined by the compiler? {}", answer); // Noncompliant
    logger.warn(MY_CONST + MY_CONST, answer);
  }
}

class AnnotationMethods {
  Logger logger = LoggerFactory.getLogger(AnnotationMethods.class);
  @interface MyAnnotation {
    String someOtherValue();
  }
  void foo(MyAnnotation annotation){
    logger.info("Caching on disk @ {}", annotation.someOtherValue()); // Compliant - annotation methods are OK
  }
}
