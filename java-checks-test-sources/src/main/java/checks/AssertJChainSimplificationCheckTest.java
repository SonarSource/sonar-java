/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

public class AssertJChainSimplificationCheckTest {

  private Object getObject() {
    return new Object();
  }

  private boolean getBoolean() {
    return true;
  }

  private String getString() {
    return "a string";
  }

  void edgeCases() {
    Object x = "x", y = "y";

    assertThat((x == y)).isTrue(); // Noncompliant
    assertThat((((((x == y)))))).isTrue(); // Noncompliant
    assertThat((x).equals(y)).isTrue(); // Noncompliant
    assertThat(x != (null)).isFalse(); // Noncompliant
    assertThat(((x).equals(((y))))).isTrue(); // Noncompliant
    assertThatObject(x.equals(y)).isEqualTo(true); // Noncompliant
    org.assertj.core.api.AssertionsForInterfaceTypes.assertThat(x == y).isTrue(); // Noncompliant
    org.assertj.core.api.AssertionsForClassTypes.assertThat(x == y).isTrue(); // Noncompliant
    assertThat(!getString().equals(x)).isTrue(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isNotEqualTo(expected) instead}}
    assertThat(!getString().equals(x)).isFalse(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isEqualTo(expected) instead}}
    assertThat(!!getString().equals(x)).isTrue(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isEqualTo(expected) instead}}
    assertThat(!!getString().equals(x)).isFalse(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isNotEqualTo(expected) instead}}
  }

  void objectRelatedAssertionChains() {
    Comparable x = getBoolean();
    Object y = getObject();

    assertThat(getObject()).isEqualTo(null); // Noncompliant [[sc=29;ec=38]] {{Use isNull() instead}}
    assertThat(getObject()).isNotEqualTo(null); // Noncompliant {{Use isNotNull() instead}}
    assertThat(getObject())
      .isEqualTo(null) // Noncompliant
      .isNotEqualTo(getObject());
    assertThat(getObject()).as("some message)").isEqualTo(getObject()).withFailMessage("another message)").isNotEqualTo(null).as("a third message"); // Noncompliant
                                                                                                                                                     // [[sc=108;ec=120]]
    assertThat(new Object()).isEqualTo(null) // Noncompliant
      .isNotEqualTo(null); // Noncompliant

    assertThat(getBoolean()).isEqualTo(true); // Noncompliant {{Use isTrue() instead}}
    assertThat(getBoolean()).isEqualTo(false); // Noncompliant {{Use isFalse() instead}}
    assertThat(x.equals(y)).isTrue(); // Noncompliant [[sc=29;ec=35;secondary=72]] {{Use assertThat(actual).isEqualTo(expected) instead}}
    assertThat(x.equals(y)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualTo(expected) instead}}
    assertThat(x == y).isTrue(); // Noncompliant {{Use assertThat(actual).isSameAs(expected) instead}}
    assertThat(x == y).isFalse(); // Noncompliant {{Use assertThat(actual).isNotSameAs(expected) instead}}
    assertThat(x != y).isTrue(); // Noncompliant {{Use assertThat(actual).isNotSameAs(expected) instead}}
    assertThat(x != y).isFalse(); // Noncompliant {{Use assertThat(actual).isSameAs(expected) instead}}
    assertThat(x == null).isTrue(); // Noncompliant {{Use assertThat(actual).isNull() instead}}
    assertThat(x != null).isTrue(); // Noncompliant {{Use assertThat(actual).isNotNull() instead}}
    assertThat(null == x).isFalse(); // Noncompliant {{Use assertThat(actual).isNotNull() instead}}
    assertThat(x != null).isFalse(); // Noncompliant {{Use assertThat(actual).isNull() instead}}
    assertThat(x.toString()).isEqualTo(y); // Noncompliant {{Use assertThat(actual).hasToString(expectedString) instead}}
    assertThat(x.hashCode()).isEqualTo(y.hashCode()); // Noncompliant {{Use assertThat(actual).hasSameHashCodeAs(expected) instead}}
    assertThat(getObject() instanceof String).isTrue(); // Noncompliant {{Use assertThat(actual).isInstanceOf(ExpectedClass.class) instead}}
    assertThat(getObject() instanceof String).isFalse(); // Noncompliant {{Use assertThat(actual).isNotInstanceOf(ExpectedClass.class) instead}}

    assertThat(x.compareTo(y)).isEqualTo(0); // Noncompliant {{Use assertThat(actual).isEqualByComparingTo(expected) instead}}
    assertThat(x.compareTo(y)).isNotEqualTo(0); // Noncompliant {{Use assertThat(actual).isNotEqualByComparingTo(expected) instead}}
    assertThat(x.compareTo(y)).as("message").isNotEqualTo(0); // Noncompliant
    assertThat(x.compareTo(y)).isNotEqualTo(0).isNotEqualTo(null).isNotEqualTo(-1); // Noncompliant [[sc=48;ec=60]] - but only raise issue on the 'null' check

    assertThat(x.compareTo(y)).isZero(); // Noncompliant {{Use assertThat(actual).isEqualByComparingTo(expected) instead}}
    assertThat(x.compareTo(y)).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEqualByComparingTo(expected) instead}}
    assertThat(x.compareTo(y)).isGreaterThan(-1); // Noncompliant {{Use assertThat(actual).isGreaterThanOrEqualTo(expected) instead}}
    assertThat(x.compareTo(y)).isGreaterThan(0); // Noncompliant {{Use assertThat(actual).isGreaterThan(expected) instead}}
    assertThat(x.compareTo(y)).isGreaterThanOrEqualTo(0); // Noncompliant {{Use assertThat(actual).isGreaterThanOrEqualTo(expected) instead}}
    assertThat(x.compareTo(y)).isGreaterThanOrEqualTo(1); // Noncompliant {{Use assertThat(actual).isGreaterThan(expected) instead}}
    assertThat(x.compareTo(y)).isLessThan(1); // Noncompliant {{Use assertThat(actual).isLessThanOrEqualTo(expected) instead}}
    assertThat(x.compareTo(y)).isLessThan(0); // Noncompliant {{Use assertThat(actual).isLessThan(expected) instead}}
    assertThat(x.compareTo(y)).isLessThanOrEqualTo(0); // Noncompliant {{Use assertThat(actual).isLessThanOrEqualTo(expected) instead}}
    assertThat(x.compareTo(y)).isLessThanOrEqualTo(-1); // Noncompliant {{Use assertThat(actual).isLessThan(expected) instead}}
    assertThat(x.compareTo(y)).isNegative(); // Noncompliant {{Use assertThat(actual).isLessThan(expected) instead}}
    assertThat(x.compareTo(y)).isNotNegative(); // Noncompliant {{Use assertThat(actual).isGreaterThanOrEqualTo(expected) instead}}
    assertThat(x.compareTo(y)).isPositive(); // Noncompliant {{Use assertThat(actual).isGreaterThan(expected) instead}}
    assertThat(x.compareTo(y)).isNotPositive(); // Noncompliant {{Use assertThat(actual).isLessThanOrEqualTo(expected) instead}}
  }

  void stringRelatedAssertionChains() {
    String x = "x";

    assertThat("some string").hasSize(0); // Noncompliant {{Use isEmpty() instead}}
    assertThat(x).isEqualTo(""); // Noncompliant {{Use isEmpty() instead}}
    assertThat(getString()).isNotEqualTo(""); // Compliant to avoid FP when getString could be null or a non-empty string
    assertThat(getString().equals(x)).isTrue(); // Noncompliant {{Use assertThat(actual).isEqualTo(expected) instead}}
    assertThat(getString().equals(x)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualTo(expected) instead}}
    assertThat(getString().contentEquals(x)).isTrue(); // Noncompliant {{Use assertThat(actual).isEqualTo(expected) instead}}
    assertThat(getString().contentEquals(x)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualTo(expected) instead}}
    assertThat(getString().equalsIgnoreCase(x)).isTrue(); // Noncompliant {{Use assertThat(actual).isEqualToIgnoringCase(expected) instead}}
    assertThat(getString().equalsIgnoreCase(x)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualToIgnoringCase(expected) instead}}
    assertThat(getString().contains("some string")).isTrue(); // Noncompliant {{Use assertThat(actual).contains(expected) instead}}
    assertThat(getString().contains(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead}}
    assertThat(getString().startsWith(x)).isTrue(); // Noncompliant {{Use assertThat(actual).startsWith(expected) instead}}
    assertThat(getString().startsWith(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotStartWith(expected) instead}}
    assertThat(getString().endsWith(x)).isTrue(); // Noncompliant {{Use assertThat(actual).endsWith(expected) instead}}
    assertThat(getString().endsWith(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotEndWith(expected) instead}}
    assertThat(getString().matches(x)).isTrue(); // Noncompliant {{Use assertThat(actual).matches(expected) instead}}
    assertThat(getString().matches(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotMatch(expected) instead}}
    assertThat(getString().compareToIgnoreCase(x)).isEqualTo(0); // Noncompliant {{Use assertThat(actual).isEqualToIgnoringCase(expected) instead}}
    assertThat(getString().compareToIgnoreCase(x)).isNotEqualTo(0); // Noncompliant {{Use assertThat(actual).isNotEqualToIgnoringCase(expected) instead}}
    assertThat(getString().compareToIgnoreCase(x)).isZero(); // Noncompliant {{Use assertThat(actual).isEqualToIgnoringCase(expected) instead}}
    assertThat(getString().compareToIgnoreCase(x)).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEqualToIgnoringCase(expected) instead}}

    assertThat(getString().indexOf(x)).isEqualTo(0); // Noncompliant {{Use assertThat(actual).startsWith(expected) instead}}
    assertThat(getString().indexOf(x)).isNotEqualTo(0); // Noncompliant {{Use assertThat(actual).doesNotStartWith(expected) instead}}
    assertThat(getString().indexOf(x)).isZero(); // Noncompliant {{Use assertThat(actual).startsWith(expected) instead}}
    assertThat(getString().indexOf(x)).isNotZero(); // Noncompliant {{Use assertThat(actual).doesNotStartWith(expected) instead}}
    assertThat(getString().indexOf(x)).isEqualTo(-1); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead}}
    assertThat(getString().indexOf(x)).isNegative(); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead}}
    assertThat(getString().indexOf(x)).isLessThan(0); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead}}
    assertThat(getString().indexOf(x)).isLessThanOrEqualTo(-1); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead}}
    assertThat(getString().indexOf(x)).isNotNegative(); // Noncompliant {{Use assertThat(actual).contains(expected) instead}}
    assertThat(getString().indexOf(x)).isGreaterThanOrEqualTo(0); // Noncompliant {{Use assertThat(actual).contains(expected) instead}}
    assertThat(getString().indexOf(x)).isGreaterThan(-1); // Noncompliant {{Use assertThat(actual).contains(expected) instead}}

    assertThat(getString().trim()).isNotEmpty(); // Noncompliant {{Use assertThat(actual).isNotBlank() instead}}
    assertThat(getString().trim()).isNotEqualTo(""); // Noncompliant {{Use assertThat(actual).isNotBlank() instead}}
    assertThat(getString().length()).isEqualTo(0); // Noncompliant {{Use assertThat(actual).isEmpty() instead}}
    assertThat(getString().length()).isLessThanOrEqualTo(0); // Noncompliant {{Use assertThat(actual).isEmpty() instead}}
    assertThat(getString().length()).isLessThan(1); // Noncompliant {{Use assertThat(actual).isEmpty() instead}}
    assertThat(getString().length()).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead}}
    assertThat(getString().length()).isEqualTo(x); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead}}
    assertThat(getString().isEmpty()).isTrue(); // Noncompliant {{Use assertThat(actual).isEmpty() instead}}
    assertThat(getString().isEmpty()).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead}}
    assertThat(getString().length()).isEqualTo(x.length()); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead}}
  }

  void compliantChains() {
    Comparable x = getBoolean();
    Object y = getObject();

    assertThat(x.compareTo(y)).isNotEqualTo(0).isNotEqualTo(-1); // Compliant as we have >1 context-dependant predicate
    assertThat(x.compareTo(y)).isOne();
    assertThat(x).extracting("test").isEqualTo("test"); // Compliant as we ignore chains containing methods that change the assertion context (too complex)
    assertThat(true).isTrue();
    assertThat(getObject()).isNull();
    assertThat(x).isSameAs(y);
    assertThat(x).isLessThanOrEqualTo(0);
    assertThat(x.compareTo(y)).as("message"); // Compliant. Not a proper test, as the predicate is missing, but this is handled elsewhere.
    assertThat(x).isNotEqualByComparingTo(0);
    assertThat(x).hasToString("expected");
    assertThat(x).hasSameHashCodeAs(y);
    assertThat(x).isInstanceOf(Object.class);
  }
}
