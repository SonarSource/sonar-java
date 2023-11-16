package checks.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class JUnitCompatibleAnnotationsCheck {

  @Test
  @RepeatedTest(2)
  void test() { // Noncompliant [[sc=8;ec=12;secondary=10,11]]{{Remove one of these conflicting annotations.}}
  }

  @RepeatedTest(4)
  void test2() { // Compliant
  }

  @ParameterizedTest
  @Test
  @MethodSource("methodSource")
  void test3(int argument) {  // Noncompliant
  }

}
