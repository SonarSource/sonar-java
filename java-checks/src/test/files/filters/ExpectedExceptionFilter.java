package filters;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

class ExpectedExceptionFilter {

  private final Instant instant = Instant.now();
  private final LocalDate date = LocalDate.now();
  private final LocalDateTime dateTime = LocalDateTime.now();

  void regularCode() {
    Instant.from(date); // WithIssue
  }

  @org.junit.Test(expected = DateTimeException.class)
  public void junit4ExpectedDateTimeException() {
    Instant.from(date); // NoIssue
  }

  @org.junit.Test(expected = DateTimeParseException.class)
  public void junit4ExpectedDateTimeExceptionSubtype() {
    Instant.from(date); // NoIssue
  }

  @org.junit.Test(expected = IllegalArgumentException.class)
  public void junit4ExpectedUnrelatedException() {
    Instant.from(date); // WithIssue
  }

  @org.testng.annotations.Test(expectedExceptions = DateTimeException.class)
  public void testngExpectedDateTimeException() {
    LocalDate.from(instant); // NoIssue
  }

  @org.testng.annotations.Test(expectedExceptions = {RuntimeException.class})
  public void testngExpectedBroadException() {
    ZonedDateTime.from(dateTime); // NoIssue
  }

  @org.testng.annotations.Test(expectedExceptions = {IllegalArgumentException.class})
  public void testngExpectedUnrelatedException() {
    OffsetDateTime.from(instant); // WithIssue
  }

  @org.testng.annotations.Test(expectedExceptions = {IllegalArgumentException.class, DateTimeException.class})
  public void testngExpectedMultipleException() {
    OffsetDateTime.from(instant); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void junit5AssertThrowsExpectedDateTimeException() {
    org.junit.jupiter.api.Assertions.assertThrows(DateTimeException.class, () -> Instant.from(date)); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void junit5AssertThrowsExpectedBroadException() {
    org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> LocalDate.from(instant)); // NoIssue
    org.junit.jupiter.api.Assertions.assertThrows(() -> LocalDate.from(instant)); // NoIssue
    org.junit.jupiter.api.Assertions.assertThrowsExactly(DateTimeException.class, () -> LocalDateTime.from(instant)); // NoIssue
    org.junit.jupiter.api.Assertions.assertThrowsExactly(DateTimeParseException.class, () -> LocalDateTime.from(instant)); // NoIssue
    org.junit.jupiter.api.Assertions.assertThrowsExactly(RuntimeException.class, () -> LocalDateTime.from(instant)); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void junit5AssertThrowsExpectedUnrelatedException() {
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ZonedDateTime.from(dateTime)); // WithIssue
  }

  @org.junit.jupiter.api.Test
  void junit5AssertThrowsOnlyFiltersExecutable() {
    org.junit.jupiter.api.Assertions.assertThrows(DateTimeException.class, () -> Instant.from(date)); // NoIssue
    Instant.from(date); // WithIssue
  }

  @org.junit.jupiter.api.Test
  void junit4AssertThrowsWithMessage() {
    org.junit.Assert.assertThrows("message", DateTimeException.class, () -> Instant.from(date)); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void testngExpectThrows() {
    org.testng.Assert.expectThrows(DateTimeException.class, () -> OffsetTime.from(instant)); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void assertjAssertThatThrownBy() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> Instant.from(date)).isInstanceOf(DateTimeException.class); // NoIssue
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> Instant.from(date)).isInstanceOf(RuntimeException.class); // NoIssue
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> Instant.from(date)).isExactlyInstanceOf(RuntimeException.class); // NoIssue
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> Instant.from(date)).isExactlyInstanceOf(DateTimeException.class); // NoIssue
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> Instant.from(date)).isInstanceOf(IllegalArgumentException.class); // WithIssue
  }

  @org.junit.jupiter.api.Test
  void assertjAssertThatCode() {
    org.assertj.core.api.Assertions.assertThatCode(() -> LocalDate.from(instant)).isInstanceOf(RuntimeException.class); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void assertjExceptionOfType() {
    org.assertj.core.api.Assertions.assertThatExceptionOfType(DateTimeException.class).isThrownBy(() -> ZonedDateTime.from(dateTime)); // NoIssue
    org.assertj.core.api.BDDAssertions.thenExceptionOfType(DateTimeException.class).isThrownBy(() -> OffsetDateTime.from(instant)); // NoIssue
  }

  @org.junit.jupiter.api.Test
  void assertjCatchThrowableOfType() {
    org.assertj.core.api.Assertions.catchThrowableOfType(() -> OffsetTime.from(instant), DateTimeException.class); // NoIssue
    org.assertj.core.api.Assertions.catchThrowableOfType(() -> Instant.from(date), IllegalArgumentException.class); // WithIssue
  }

  @org.junit.jupiter.api.Test
  void assertjTypedShortcuts() {
    org.assertj.core.api.Assertions.assertThatRuntimeException().isThrownBy(() -> Instant.from(date)); // NoIssue
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException().isThrownBy(() -> Instant.from(date)); // WithIssue
    BDDAssertions.thenRuntimeException().isThrownBy(() -> Instant.from(date)); // NoIssue

  }

  @org.junit.jupiter.api.Test
  void tryCatchFail() {
    try {
      Instant.from(date); // NoIssue
      org.junit.Assert.fail();
    } catch (DateTimeException e) {
      // expected
    }

    try {
      LocalDate.from(instant); // NoIssue
      org.assertj.core.api.Assertions.fail("expected exception");
    } catch (RuntimeException e) {
      // expected
    }

    try {
      Instant.from(date); // WithIssue
      org.junit.Assert.fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      Instant.from(date); // NoIssue
      org.junit.Assert.fail();
    } catch (DateTimeException | IllegalArgumentException e) {
      // expected
    }

    try {
      LocalDate.from(instant); // NoIssue
    } catch (DateTimeException e) {
      // expected
    }
  }

  // When semantic information is missing, the filter should conservatively activate.
  @Test(expected = RuntimeException.class)
  void expectedMethodAnnotationWithoutSemantics() {
    Instant.from(date); // NoIssue
  }

  @Test(expectedExceptions = DateTimeException.class)
  void expectedExceptionsMethodAnnotationWithoutSemantics() {
    Instant.from(date); // NoIssue
  }

  @Test
  void methodInvocationsWithoutSemantics() {
    // Here too, the filter should activate even though no semantic information is available.
    assertThrows(DateTimeException.class, () -> LocalDate.from(instant)); // NoIssue
    assertThatThrownBy(() -> Instant.from(date)).isInstanceOf(DateTimeException.class); // NoIssue
    assertThatExceptionOfType(DateTimeException.class).isThrownBy(() -> ZonedDateTime.from(dateTime)); // NoIssue
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ZonedDateTime.from(dateTime)); // WithIssue
    assertThatIllegalArgumentException().isThrownBy(() -> Instant.from(date)); // WithIssue
    thenRuntimeException().isThrownBy(() -> Instant.from(date)); // NoIssue
  }

  @Test(expected = DateTimeException)
  void wrongExpectedType() {
    Instant.from(date); // WithIssue
  }

  @Test(expect = DateTimeException.class)
  void wrongAnnotationAttribute() {
    Instant.from(date); // WithIssue
  }

  @Test
  void wrongAssertMethodSignature() {
    assertThrows(Instant.from(date)); // WithIssue
    catchThrowableOfType(Instant.from(date)); // WithIssue
  }

}
