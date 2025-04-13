package symbolicexecution.checks.ParameterNullnessCheck.packageNonNull;

import com.google.common.base.Preconditions;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Package is annotated with @javax.annotation.ParametersAreNonnullByDefault, every parameters in this file are nonnull by default.
 */
abstract class ParameterNullnessCheckSample {
  public void test() {
//    new IssueRelease("a"); // Compliant
//    new IssueRelease( null); // Compliant
//    var param = new Random().nextInt(2) == 0 ? null : "a";
    String param = null;
//    fn(param); // Compliant
    new IssueRelease(param); // Compliant
  }

//  public void fn(String param1) {
//  }

}

public record IssueRelease(
  @Nullable String param1
) {
  public IssueRelease {
    var a = new IssueRelease(null);
  }
}
