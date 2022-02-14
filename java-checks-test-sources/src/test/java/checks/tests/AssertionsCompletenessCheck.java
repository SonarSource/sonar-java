package checks.tests;

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
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

public class AssertionsCompletenessCheck {
  @Rule
  public final org.assertj.core.api.JUnitSoftAssertions junit_soft_assertions = new org.assertj.core.api.JUnitSoftAssertions();

  @Test
  public void fest_assertions() {
    org.fest.assertions.Assertions.assertThat(true); // Noncompliant {{Complete the assertion.}}
    org.fest.assertions.Assertions.assertThat(true).as("foo"); // Noncompliant
    org.fest.assertions.Assertions.assertThat(true).describedAs("foo");  // Noncompliant
    org.fest.assertions.Assertions.assertThat(true).overridingErrorMessage("foo");  // Noncompliant

    org.fest.assertions.Assertions.assertThat(true).isTrue(); // Compliant
    org.fest.assertions.Assertions.assertThat(AssertionsCompletenessCheck.class.toString()).hasSize(0); // Compliant
    org.fest.assertions.Assertions.assertThat(AssertionsCompletenessCheck.class.toString()).as("aa").hasSize(0); // Compliant
  }

  private BooleanAssert return_fest_assertion(String filename, String key) {
    // Compliant, no issue is raised for return statements and variable assignments to allow helper methods
    BooleanAssert result = org.fest.assertions.Assertions.assertThat(filename.contains(key));
    return org.fest.assertions.Assertions.assertThat(filename.contains(key));
  }

  @Test
  public void call_fest_assertion_builder() {
    return_fest_assertion("foo.txt", "key1").isTrue();
    return_fest_assertion("bar.txt", "key2").isTrue();
  }

  @Test
  public void mockito_assertions() {
    List<String> mockedList = Mockito.mock(List.class);

    Mockito.verify(mockedList); // Noncompliant
    Mockito.verify(mockedList, Mockito.times(0)); // Noncompliant

    Mockito.verify(mockedList).add("one");
    Mockito.verify(mockedList, Mockito.times(0)).clear();
    Mockito.verifyNoMoreInteractions(mockedList);
    Mockito.verifyZeroInteractions(mockedList);
  }

  @Test
  public void junit_assertions() {
    org.junit.Assert.assertThat(3, org.hamcrest.Matchers.is(3));
  }

  @Test
  public void google_truth_assertions() {
    boolean b = true;
    Truth.assertThat(b).isTrue();
    String s = "Hello Truth Framework World!";
    Truth.assertThat(s).contains("Hello");
    Truth.assertThat(b); // Noncompliant
    Truth.assertWithMessage("Invalid option").that(b).isFalse();
    Truth.assertWithMessage("Invalid option").that(b); // Noncompliant
  }

  @Test
  public void google_truth8_assertions() {
    Truth8.assertThat(Stream.of(1, 2, 3)); // Noncompliant
    Truth8.assertThat(Stream.of(1, 2, 3)).containsAllOf(1, 2, 3).inOrder();
    boolean b = true;
    Truth8.assertThat(Optional.of(b)); // Noncompliant
    Truth8.assertThat(Optional.of(b)).isPresent();
    Truth8.assertThat(OptionalInt.of(1)); // Noncompliant
    Truth8.assertThat(OptionalInt.of(1)).hasValue(0);
  }

  @Test
  public void assertj_assertions() {
    org.assertj.core.api.Assertions.assertThat(1).isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(1); // Noncompliant
    org.assertj.core.api.Assertions.assertThat(1).withThreadDumpOnError().isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(1).withThreadDumpOnError(); // Noncompliant
    org.assertj.core.api.Assertions.assertThat(1).overridingErrorMessage("error").isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(1).overridingErrorMessage("error"); // Noncompliant
    org.assertj.core.api.Assertions.assertThat(1).usingDefaultComparator().isGreaterThan(0);
    org.assertj.core.api.Assertions.assertThat(1).usingDefaultComparator(); // Noncompliant
    org.assertj.core.api.Assertions.assertThatObject(null).extracting("name"); // Noncompliant
    org.assertj.core.api.Assertions.assertThatObject(null).extracting("name").isEqualTo("Paul");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> {}).describedAs(""); // Compliant, can be used alone (will fail if not exception is used).
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> {}).describedAs("").hasMessage("42");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> {}, "desc", 42).usingComparator(null); // Compliant
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> {}, "desc", 42).usingComparator(null).isInstanceOf(IllegalStateException.class);
    org.assertj.core.api.Assertions.assertThatCode(() -> {}); // Noncompliant
    org.assertj.core.api.Assertions.assertThatCode(() -> {}).isInstanceOf(IllegalStateException.class);
    org.assertj.core.api.Assertions.assertThatExceptionOfType(IllegalStateException.class); // Noncompliant
    org.assertj.core.api.Assertions.assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {});
    org.assertj.core.api.Assertions.assertThatNullPointerException(); // Noncompliant
    org.assertj.core.api.Assertions.assertThatNullPointerException().isThrownBy(() -> {});
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException(); // Noncompliant
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException().isThrownBy(() -> {});
    org.assertj.core.api.Assertions.assertThatIOException(); // Noncompliant
    org.assertj.core.api.Assertions.assertThatIOException().isThrownBy(() -> {});
    org.assertj.core.api.Assertions.assertThatIllegalStateException(); // Noncompliant
    org.assertj.core.api.Assertions.assertThatIllegalStateException().isThrownBy(() -> {});

    Comparator customComparator = null;
    org.assertj.core.api.Assertions.assertThat(1).usingComparator(customComparator).isGreaterThanOrEqualTo(0);
    org.assertj.core.api.Assertions.assertThat(1).usingComparator(customComparator); // Noncompliant
    org.assertj.core.api.Assertions.assertThat("a").asString().hasSize(1);
    org.assertj.core.api.Assertions.assertThat("a").asString(); // Noncompliant
    List a = null;
    org.assertj.core.api.Assertions.assertThat(a).asList().hasSize(0);
    org.assertj.core.api.Assertions.assertThat(a).asList(); // Noncompliant
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat((Path) null); // Noncompliant
    softly.assertAll();
  }

  @Test
  public void assertj_java6assertions() {
    org.assertj.core.api.Java6Assertions.assertThat(1).isGreaterThan(0);
    org.assertj.core.api.Java6Assertions.assertThat(1); // Noncompliant
    org.assertj.core.api.Java6Assertions.assertThat(1).withThreadDumpOnError().isGreaterThan(0);
    org.assertj.core.api.Java6Assertions.assertThat(1).withThreadDumpOnError(); // Noncompliant
    org.assertj.core.api.Java6Assertions.assertThat(1).overridingErrorMessage("error").isGreaterThan(0);
    org.assertj.core.api.Java6Assertions.assertThat(1).overridingErrorMessage("error"); // Noncompliant
    org.assertj.core.api.Java6Assertions.assertThat(1).usingDefaultComparator().isGreaterThan(0);
    org.assertj.core.api.Java6Assertions.assertThat(1).usingDefaultComparator(); // Noncompliant
    Comparator customComparator = null;
    org.assertj.core.api.Java6Assertions.assertThat(1).usingComparator(customComparator).isGreaterThanOrEqualTo(0);
    org.assertj.core.api.Java6Assertions.assertThat(1).usingComparator(customComparator); // Noncompliant
    org.assertj.core.api.Java6Assertions.assertThat("a").asString().hasSize(1);
    org.assertj.core.api.Java6Assertions.assertThat("a").asString(); // Noncompliant
    List a = null;
    org.assertj.core.api.Java6Assertions.assertThat(a).asList().hasSize(0);
    org.assertj.core.api.Java6Assertions.assertThat(a).asList(); // Noncompliant
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat((String) null); // Noncompliant
    softly.assertAll();
  }

  @Test
  public void assertj_soft_assertions_without_assertAll() {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat(5).isLessThan(3);
    softly.assertThat(1).isGreaterThan(2);
  } // Noncompliant {{Add a call to 'assertAll' after all 'assertThat'.}}

  @Test
  void assertj_java6_soft_assertions_without_assertAll() {
    org.assertj.core.api.Java6SoftAssertions softly = new org.assertj.core.api.Java6SoftAssertions();
    softly.assertThat(new A()); // Noncompliant
    softly.assertThat(5).isLessThan(3);
  } // Noncompliant

  @Test
  public void assertj_soft_assertions_ok() {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat(5).isLessThan(3);
    softly.assertThat(1).isGreaterThan(2);
    softly.assertAll();
  }

  @Test
  void assertj_java6_soft_assertions_ok() {
    org.assertj.core.api.Java6SoftAssertions softly = new org.assertj.core.api.Java6SoftAssertions();
    softly.assertThat(5).isLessThan(3);
    softly.assertAll();
  }

  @Test
  public void assertj_soft_assertions_without_assertThat() {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertAll(); // Noncompliant {{Add one or more 'assertThat' before 'assertAll'.}}
  }

  @Test
  public void assertj_soft_assertions_try_with_resource() {
    try(org.assertj.core.api.AutoCloseableSoftAssertions softly = new org.assertj.core.api.AutoCloseableSoftAssertions()) {
      softly.assertThat(1).isLessThan(2);
    } // Compliant, no need to call "assertAll()", it will be called by AutoCloseableSoftAssertions
  }

  @Test
  public void assertj_soft_assertions_try_with_resource_without_assertThat() {
    try(org.assertj.core.api.AutoCloseableSoftAssertions softly = new org.assertj.core.api.AutoCloseableSoftAssertions()) {
    } // Noncompliant {{Add one or more 'assertThat' before the end of this try block.}}
  }

  @Test
  public void assertj_soft_assertions_try_with_resource_with_useless_assertAll() {
    try(org.assertj.core.api.AutoCloseableSoftAssertions softly = new org.assertj.core.api.AutoCloseableSoftAssertions()) {
      softly.assertThat(1).isLessThan(2);
      softly.assertAll();
    } // Noncompliant {{Add one or more 'assertThat' before the end of this try block.}}
  }

  @Test
  public void assertj_junit_soft_assertions() {
    junit_soft_assertions.assertThat(1).isLessThan(2);
  } // Compliant, no need to call "assertAll()", it will be called by the @Rule of junit_soft_assertions

  @Test
  public void assertj_soft_assertions_try_with_resource_java9() {
    try(org.assertj.core.api.AutoCloseableSoftAssertions softly = new org.assertj.core.api.AutoCloseableSoftAssertions()) {
      softly.assertThat(1).isLessThan(2);
    } // Compliant, no need to call "assertAll()", it will be called by AutoCloseableSoftAssertions
  }

  @Test
  public void assertj_junit_soft_assertions_cross_methods_1() throws Exception {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    doSomething(softly);
    softly.assertAll();
  }

  @Test
  public void assertj_junit_soft_assertions_cross_methods_2() throws Exception {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat(1).isEqualTo("1");
    doSomethingElse(softly);
  }

  @Test
  public void assertj_junit_soft_assertions_cross_methods_3() throws Exception {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    doBoth(softly, true);
  }

  @Test
  public void assertj_junit_soft_assertions_cross_methods_4() throws Exception {
    doSoftAssertions("expected");
  }

  @Test
  public void assertj_junit_soft_assertions_cross_methods_5() throws Exception {
    doIncompleteSoftAssertions1("expected");
  } // Noncompliant {{Add a call to 'assertAll' after all 'assertThat'.}}

  @Test
  public void assertj_junit_soft_assertions_cross_methods_6() throws Exception {
    doIncompleteSoftAssertions2(); // Noncompliant [[sc=5;ec=34;secondary=277,282]] {{Add one or more 'assertThat' before 'assertAll'.}}
  }

  private void doSomething(org.assertj.core.api.SoftAssertions softly) {
    softly.assertThat(1).isEqualTo("1");
  }

  private void doSomethingElse(org.assertj.core.api.SoftAssertions softly) {
    softly.assertAll();
  }

  private void doSoftAssertions(String expected) {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat(1).isEqualTo(expected);
    softly.assertAll();
  }

  private void doIncompleteSoftAssertions1(String expected) {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertThat(1).isEqualTo(expected);
  }

  private void doIncompleteSoftAssertions2() {
    doIncompleteSoftAssertions3();
  }

  private void doIncompleteSoftAssertions3() {
    org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
    softly.assertAll();
  }

  private void doBoth(org.assertj.core.api.SoftAssertions softly, boolean doItAgain) {
    doSomething(softly);
    if (doItAgain) {
      doBoth(softly, !doItAgain);
    }
    doSomethingElse(softly);
  }

  @Test
  public void assertj_soft_assertions_with_assert_softly() {
    org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(5).isLessThan(3);
      softly.assertThat(1).isGreaterThan(2);
      // Compliant the "assertAll" method will be called automatically
    });
  }

  @Test
  public void assertj_soft_assertions_mixing_assert_softly_and_assert_all_1() {
    org.assertj.core.api.SoftAssertions mainSoftly = new org.assertj.core.api.SoftAssertions();
    mainSoftly.assertThat(5).isLessThan(3);
    org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(5).isLessThan(3);
    });
    mainSoftly.assertAll();
  }

  @Test
  public void assertj_soft_assertions_mixing_assert_softly_and_assert_all_2() {
    org.assertj.core.api.SoftAssertions mainSoftly = new org.assertj.core.api.SoftAssertions();
    org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(5).isLessThan(3);
    });
    // missing "assertThat"
    mainSoftly.assertAll(); // Noncompliant
  }

  static class A {
  }

}

@ExtendWith(value = SoftAssertionsExtension.class)
class JUnit5SoftAssertionsExample {
  @Test
  void junit5_soft_assertions_example(org.assertj.core.api.SoftAssertions softly) {
    softly.assertThat(5).isLessThan(3);
    // No need to call softly.assertAll(), this is automatically done by the SoftAssertionsExtension
  }
}

@ExtendWith({MyExtension.class, SoftAssertionsExtension.class})
class JUnit5SoftAssertionsExample2 {
  @Test
  void junit5_soft_assertions_example(org.assertj.core.api.SoftAssertions softly) {
    softly.assertThat(5).isLessThan(3);
  }
}


@ExtendWith(MyExtension.class)
class JUnit5SoftAssertionsExample3 {
  @Test
  void junit5_soft_assertions_example(org.assertj.core.api.SoftAssertions softly) {
    softly.assertThat(5).isLessThan(3);
  } // Noncompliant
}

class MyExtension implements AfterTestExecutionCallback {
  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
  }
}

@ExtendWith(SoftAssertionsExtension.class)
class NestedJUnit5SoftAssertionsExample {
  @Nested
  class NestedClass {
    @Test
    void junit5_soft_assertions_example(org.assertj.core.api.SoftAssertions softly) {
      softly.assertThat(5).isLessThan(3);
      // No need to call softly.assertAll(), this is automatically done by the SoftAssertionsExtension
    }
  }
}
