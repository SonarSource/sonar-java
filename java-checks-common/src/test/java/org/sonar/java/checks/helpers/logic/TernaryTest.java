package org.sonar.java.checks.helpers.logic;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.helpers.logic.Ternary.FALSE;
import static org.sonar.java.checks.helpers.logic.Ternary.TRUE;
import static org.sonar.java.checks.helpers.logic.Ternary.UNKNOWN;
import static org.sonar.java.checks.helpers.logic.Ternary.and;
import static org.sonar.java.checks.helpers.logic.Ternary.or;


class TernaryTest {
  @Test
  void test_from_boolean_conversion() {
    assertThat(Ternary.of(true)).isEqualTo(TRUE);
    assertThat(Ternary.ofNullable(true)).isEqualTo(TRUE);

    assertThat(Ternary.of(false)).isEqualTo(FALSE);
    assertThat(Ternary.ofNullable(false)).isEqualTo(FALSE);

    assertThat(Ternary.ofNullable(null)).isEqualTo(UNKNOWN);
  }

  @Test
  void test_or() {
    assertThat(or(TRUE, FALSE, UNKNOWN)).isEqualTo(TRUE);
    assertThat(or(FALSE, UNKNOWN)).isEqualTo(UNKNOWN);
    assertThat(or(FALSE, FALSE)).isEqualTo(FALSE);
  }

  @Test
  void test_and() {
    assertThat(and(TRUE, FALSE, UNKNOWN)).isEqualTo(FALSE);
    assertThat(and(TRUE, UNKNOWN)).isEqualTo(UNKNOWN);
    assertThat(and(TRUE, TRUE)).isEqualTo(TRUE);
  }

  @Test
  void test_collectors() {
    List<Ternary> tfu = List.of(TRUE, FALSE, UNKNOWN);
    assertThat(tfu.stream().collect(and())).isEqualTo(FALSE);
    assertThat(tfu.stream().collect(or())).isEqualTo(TRUE);

    List<Ternary> uut = List.of(UNKNOWN, UNKNOWN, TRUE);
    assertThat(uut.stream().collect(and())).isEqualTo(UNKNOWN);
    assertThat(uut.stream().collect(or())).isEqualTo(TRUE);
  }
}
