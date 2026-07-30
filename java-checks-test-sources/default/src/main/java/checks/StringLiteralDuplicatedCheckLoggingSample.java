package checks;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;

/**
 * Verifies that S1192 does not flag string literals used as logging arguments.
 * Logging calls (SLF4J, JUL, Log4j) are commonly repeated with the same message — extracting
 * them to constants is impractical and not idiomatic, so they are intentionally suppressed.
 */
class SuppressedBySlf4j {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SuppressedBySlf4j.class);

  void doStuff() {
    LOG.info("operation failed");  // Compliant — repeated log messages are excluded
    LOG.warn("operation failed");
    LOG.error("operation failed");
  }
}

class SuppressedByJul {

  private static final Logger LOG = Logger.getLogger(SuppressedByJul.class.getName());

  void doStuff() {
    LOG.warning("operation failed");  // Compliant — repeated JUL messages are excluded
    LOG.log(Level.WARNING, "operation failed");
    LOG.severe("operation failed");
  }
}

class SuppressedByLog4j {

  private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(SuppressedByLog4j.class);

  void doStuff() {
    LOG.info("operation failed");  // Compliant — repeated Log4j messages are excluded
    LOG.warn("operation failed");
    LOG.error("operation failed");
  }
}

class MixedLoggingAndNonLogging {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MixedLoggingAndNonLogging.class);

  void doStuff(String v1, String v2, String v3) {
    LOG.error("validation error"); // Compliant — the logging occurrence does not count
    doValidate("validation error", v1); // Noncompliant {{Define a constant instead of duplicating this literal "validation error" 3 times.}}
//             ^^^^^^^^^^^^^^^^^^
    doValidate("validation error", v2);
//             ^^^^^^^^^^^^^^^^^^<
    doValidate("validation error", v3);
//             ^^^^^^^^^^^^^^^^^^<
  }

  private void doValidate(String msg, String value) {}
}
