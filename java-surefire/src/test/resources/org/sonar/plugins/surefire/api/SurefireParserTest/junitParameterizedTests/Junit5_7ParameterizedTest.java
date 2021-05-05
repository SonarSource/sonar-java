package org.foo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class Junit5_7ParameterizedTest {

  static class A {
    public String bar(String s) {
      return s;
    }
  }

  // can not be executed by junit5 test runner
  @org.junit.Test
  public void myNormalJunit4Test() {
    assertThat(new A()).isNotNull();
  }

  @Test
  void myNormalJunit5Test() {
    assertThat(new A()).isNotNull();
  }

  @RepeatedTest(value = 3, name = "Repetition #{currentRepetition} / {totalRepetitions}")
  void myRepeatedTest() {
    assertThat(new A()).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "hello",
    "world"
  })
  void myParametrizedTest(String s) {
    assertThat(s).isNotNull();
    assertThat(new A().bar(s)).isEqualTo(s);
  }

  @Nested
  class RunTest {

    @Test
    void myNormalJunit5Test() {
      assertThat(new A()).isNotNull();
    }

    @RepeatedTest(value = 3, name = "Repetition #{currentRepetition} / {totalRepetitions}")
    void myRepeatedTest() {
      assertThat(new A()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "hello",
      "world"
    })
    void myParametrizedTest(String s) {
      assertThat(s).isNotNull();
      assertThat(new A().bar(s)).isEqualTo(s);
    }
  }
}
