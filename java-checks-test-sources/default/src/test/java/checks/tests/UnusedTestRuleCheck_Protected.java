package checks.tests;

import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

// https://community.sonarsource.com/t/possible-fp-for-java-tempdir-declared-in-super-class/60836
// https://sonarsource.atlassian.net/browse/SONARJAVA-4238
public abstract class UnusedTestRuleCheck_Protected {
  @TempDir
  protected Path tempDirOldFP; // Compliant FP as used in a subclass - compliant because not private
}
