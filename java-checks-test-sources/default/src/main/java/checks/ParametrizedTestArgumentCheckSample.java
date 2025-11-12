package checks;

import java.time.Month;
import java.util.EnumSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParametrizedTestArgumentCheckSample {
  @ParameterizedTest
  @ValueSource(strings = {"a", "b", "c"})
  void test1(String value) { // Compliant
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "b", "c"})
  void test2() { // Noncompliant
  }

  @ParameterizedTest
  @CsvSource(value = {"test:test", "tEst:test", "Java:java"}, delimiter = ':')
  void test3(String input, String expected) { // Compliant
    String actualValue = input.toLowerCase();
    assertEquals(expected, actualValue);
  }

  @ParameterizedTest
  @CsvSource(value = {"test:test", "tEst:test", "Java:java"}, delimiter = ':')
  void test4(String input) { // Noncompliant
    String actualValue = input.toLowerCase();
  }

  @ParameterizedTest
  @CsvSource(value = {"test:test", "tEst:test", "Java:java"}, delimiter = ':')
  void test5(String input, String expected, String somemore) { // TODO ?
    String actualValue = input.toLowerCase();
    assertEquals(expected, actualValue);
  }

  @ParameterizedTest
  @CsvSource(value = {"test:test", "tEst:test", "Java:java"}, delimiter = ':')
  @ValueSource(strings = {"a", "b", "c"})
  void test5_2(String input, String expected, String somemore) { // TODO ?
    String actualValue = input.toLowerCase();
    assertEquals(expected, actualValue);
  }

  @ParameterizedTest
  @CsvSource(value = {"test:test:x", "tEst:test:y", "Java:java:z"}, delimiter = ':')
  void test6(String input, String expected, String index) { // Compliant
    String actualValue = input.toLowerCase();
    assertEquals(expected, actualValue);
  }

  @ParameterizedTest
  @CsvSource(value = {"test:test:x", "tEst:test:y", "Java:java:z"}, delimiter = ':')
  void test7(String input, String expected) { // Noncompliant
    String actualValue = input.toLowerCase();
    assertEquals(expected, actualValue);
  }


  @ParameterizedTest
  @EnumSource(value = Month.class, names = ".+BER", mode = EnumSource.Mode.MATCH_ANY)
  void test8(Month month) { // Compliant
    EnumSet<Month> months =
      EnumSet.of(Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER);
    assertTrue(months.contains(month));
  }

  @ParameterizedTest
  @EnumSource(value = Month.class, names = ".+BER", mode = EnumSource.Mode.MATCH_ANY)
  void test9(Month month) { // Noncompliant
    EnumSet<Month> months =
      EnumSet.of(Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER);
    assertTrue(months.contains(month));
  }

}
