package checks.tests;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AssertJAssertionsInConsumerCheck {

  private static final Consumer<String> classRequirements = s -> assertThat(s).isEqualTo("b");

  @Test
  public void testIsInstanceOfSatisfying(Consumer<String> unknownRequirements) {
    Object myObj = getSomeObject();
    assertThat(myObj).isInstanceOfSatisfying(String.class, "b"::equals); // Noncompliant
    assertThat(myObj).isInstanceOfSatisfying(String.class, somethingUnknown()); // Compliant, unknown method call
  }

  protected abstract Object getSomeObject();
}
