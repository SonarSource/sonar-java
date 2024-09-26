package checks.tests;

import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

// https://community.sonarsource.com/t/possible-fp-for-java-tempdir-declared-in-super-class/60836
// https://sonarsource.atlassian.net/browse/SONARJAVA-4238
public abstract class UnusedTestRuleCheck_Protected {
  @TempDir
  protected Path tempDirOldFP; // Compliant FP as used in a subclass - compliant because not private
}

// Test abstract private
abstract class AbstractTestCase {

  @TempDir
  private Path tempDir; // Noncompliant {{Remove this unused "TempDir".}}
  // increases AutoScan FNs

  void test() {
  }

}

// Test non-abstract private
class ClassTestCase { // increases AutoScan FNs

  @TempDir
  private Path tempDir; // Noncompliant {{Remove this unused "TempDir".}}
  // increases AutoScan FNs

  void test() {
  }

}

// Test non-abstract protected
class ClassTestCase2 {

  @TempDir
  protected Path tempDir; // Noncompliant {{Remove this unused "TempDir".}}
  // increases AutoScan FNs

  void test() {
  }

}
