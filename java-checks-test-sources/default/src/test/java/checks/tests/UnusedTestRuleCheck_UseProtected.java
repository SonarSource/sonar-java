package checks.tests;

import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// AutoScan S3577 Increases FN to 46
public class UnusedTestRuleCheck_UseProtected extends UnusedTestRuleCheck_Protected {
  @BeforeEach
  void setup() throws Exception {
    Files.createTempFile(tempDirOldFP, "test", "");
  }
  @Test
  void test() {
    // AutoScan S2699 Increases FN to 151
  }
}
