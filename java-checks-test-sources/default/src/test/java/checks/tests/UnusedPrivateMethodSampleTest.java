package checks.tests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class UnusedPrivateMethodSampleTest {
  private static Stream<Integer> provideData() {
    return Stream.of(1, 2, 3);
  }

  private static Stream<Integer> provideDataValue() {
    return Stream.of(1, 2, 3);
  }

  // False positive: JUnit 5 supports fully qualified method names, but we do not.
  private static Stream<Integer> provideDataFQ() { // Noncompliant
    return Stream.of(1, 2, 3);
  }

  private static Stream<Integer> provideDataArray1() {
    return Stream.of(1, 2, 3);
  }

  private static Stream<Integer> provideDataArray2() {
    return Stream.of(4, 5, 6);
  }

  private static Stream<Integer> provideDataArray1Value() {
    return Stream.of(1, 2, 3);
  }

  private static Stream<Integer> provideDataArray2Value() {
    return Stream.of(4, 5, 6);
  }

  private static Stream<Integer> notUsed() { // Noncompliant
    return Stream.of(7, 8, 9);
  }

  @ParameterizedTest
  @MethodSource("provideData")
  void test(int num) {
    fail();
  }

  @ParameterizedTest
  @MethodSource(value = "provideDataValue")
  void testValue(int num) {
    fail();
  }

  @ParameterizedTest
  @MethodSource("checks.tests.UnusedPrivateMethodSampleTest#provideDataFQ")
  void testFQ(int num) {
    fail();
  }

  @ParameterizedTest
  @MethodSource({"provideDataArray1", "provideDataArray2"})
  void testArray(int num) {
    fail();
  }

  @ParameterizedTest
  @MethodSource(value = {"provideDataArray1Value", "provideDataArray2Value"})
  void testArrayValue(int num) {
    fail();
  }
}
