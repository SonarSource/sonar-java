package checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Introduced to handle NPE described in SONARJAVA-5759
// https://sonarsource.atlassian.net/browse/SONARJAVA-5759
enum PrintfMisuseCheckEnumsSample {
  A,
  B;

  private static final Logger LOG = LoggerFactory.getLogger(PrintfMisuseCheckEnumsSample.class);

  public static PrintfMisuseCheckEnumsSample of(String name) {
    if (name.equals(B.name())) {
      LOG.error("Connector type name {}"); // Noncompliant
      return B;
    }
    if (name.equals(A.name())) {
      LOG.error("No action needed, just FYI: connector type name {}, or {}", A); // Noncompliant
      return A;
    }
    return valueOf(name);
  }
}
