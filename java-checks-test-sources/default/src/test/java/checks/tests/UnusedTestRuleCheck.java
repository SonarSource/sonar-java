package checks.tests;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;

class UnusedTestRuleCheck {

  @Rule
  public TestName testNameUnused = new TestName(); // Noncompliant [[sc=19;ec=33]] {{Remove this unused "TestName".}}

  public TestName testNameObj = new TestName();

  @Rule
  public Timeout globalTimeout = Timeout.millis(20);

  @Rule
  public TestName testNameUsed = new TestName();

  @Rule
  public TemporaryFolder tempFolderUnused = new TemporaryFolder();  // Noncompliant {{Remove this unused "TemporaryFolder".}}

  @Rule
  public TemporaryFolder tempFolderUsed = new TemporaryFolder();

  @Test
  public void testIt() throws IOException {
    tempFolderUsed.newFile();
    testNameUsed.getMethodName();
  }
}
