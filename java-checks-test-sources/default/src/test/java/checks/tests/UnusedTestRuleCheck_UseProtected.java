package checks.tests;

import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UnusedTestRuleCheck_UseProtected extends UnusedTestRuleCheck_Protected {
  @BeforeEach
  void setup() throws Exception {
    Files.createTempFile(tempDirOldFP, "test", "");
  }
  @Test
  void test() {
  }
}
