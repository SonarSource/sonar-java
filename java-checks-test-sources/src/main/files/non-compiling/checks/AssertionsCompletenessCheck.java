package checks;

import com.google.common.truth.Truth;
import com.google.common.truth.Truth8;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.fest.assertions.BooleanAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

public class AssertionsCompletenessCheck {

  @Test
  void assertj_java6_abstract_standard_soft_assertions() {
    // Java6AbstractStandardSoftAssertions was missing abstract keyword in version < 3.15, it was possible to instanciate it,
    // but it should not be treated as the others.
    org.assertj.core.api.Java6AbstractStandardSoftAssertions softly = new org.assertj.core.api.Java6AbstractStandardSoftAssertions(); // Noncompliant {{Use 'Java6SoftAssertions' instead of 'Java6AbstractStandardSoftAssertions'.}}
    softly.assertThat(5).isLessThan(3);
    softly.assertThat(5);
  }

}
