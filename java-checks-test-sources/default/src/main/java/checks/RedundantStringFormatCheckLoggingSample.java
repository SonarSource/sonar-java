package checks;

import java.util.Formattable;
import java.util.Formatter;
import java.util.Locale;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

class RedundantStringFormatCheckLoggingSample {

  private static final Logger LOG = LoggerFactory.getLogger(RedundantStringFormatCheckLoggingSample.class);
  private static final org.apache.logging.log4j.Logger LOG4J = LogManager.getLogger();
  private static final java.util.logging.Logger JUL = java.util.logging.Logger.getLogger("jul");
  private static final String FORMAT = "User %s";

  void slf4j(String username, String role, String id, Exception e, Marker marker, int count, double ratio) {
    LOG.info(String.format("Activated user '%s' with role '%s'", username, role)); // Noncompliant {{Use the logger's built-in "{}" formatting instead of "String.format".}}
//           ^^^^^^^^^^^^^
    LOG.error(String.format("Failed to process %s", id), e); // Noncompliant
    LOG.warn(marker, String.format("Retry count %d", count)); // Noncompliant
    LOG.debug(String.format("Progress 100%% for %s", id)); // Noncompliant

    LOG.info("Activated user '{}' with role '{}'", username, role);
    LOG.info("Ratio: {}", String.format("%.2f", ratio));
    LOG.info(String.format("Ratio %.2f", ratio));
    LOG.info(String.format(Locale.FRANCE, "User %s", username));
    LOG.info(String.format(FORMAT, username));
    LOG.info(String.format("User %s", username).trim());
    String message = String.format("User %s", username);
    LOG.info(message);
  }

  void formattable(FormattableId id, Formattable formattable, Object value) {
    LOG.info(String.format("Value: %s", id));
    LOG.info(String.format("Value: %s", formattable));
    LOG4J.info(String.format("Values: %s %s", value, id));
    LOG.info(String.format("Value: %s", value)); // Noncompliant
  }

  static class FormattableId implements Formattable {
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
      formatter.format("ID");
    }
  }

  void log4j(String id, int count) {
    LOG4J.debug(String.format("Loaded %s", id)); // Noncompliant
    LOG4J.fatal(String.format("Crashed after %d attempts", count)); // Noncompliant
    LOG4J.log(Level.INFO, String.format("Loaded %s", id)); // Noncompliant

    LOG4J.debug("Loaded {}", id);
    LOG4J.info(String.format("Id %x", count));
  }

  void jul(String id) {
    JUL.info(String.format("Loaded %s", id));
  }
}
