package checks;

import static org.assertj.core.api.Assertions.assertThat;

public class SimplifiableChainedAssertJAssertionsCheckTest {
  void foo() {
    assertThat(new Object()).isEqualTo(null).isNotEqualTo(null);
    assertThat(Integer.valueOf(1).compareTo(2)).isGreaterThan(1).isLessThan(10);
  }
}
