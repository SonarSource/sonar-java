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
    assertThat(!getString().equals(x)).isTrue(); // compliant as boolean negation is unsupported but should be: {{Replace with assertThat(actual).isNotEqualTo(expected)}}
    assertThat(!getString().equals(x)).isFalse(); // compliant as boolean negation is unsupported but should be: {{Replace with assertThat(actual).isEqualTo(expected)}}
    assertThat(!!getString().equals(x)).isTrue(); // compliant as boolean negation is unsupported but should be: {{Replace with assertThat(actual).isEqualTo(expected)}}
    assertThat(!!getString().equals(x)).isFalse(); // compliant as boolean negation is unsupported but should be: {{Replace with assertThat(actual).isNotEqualTo(expected)}}
  }

  void objectRelatedAssertionChains() {
    Comparable x = getBoolean();
    Object y = getObject();

    assertThat(getObject()).isEqualTo(null); // Noncompliant [[sc=29;ec=38]] {{Use isNull() instead}}
    assertThat(getObject()).isNotEqualTo(null); // Noncompliant {{Use isNotNull() instead}}
    assertThat(getObject()).isEqualTo(null).isNotEqualTo(getObject()); // Noncompliant
    assertThat(getObject()).as("some message)").isEqualTo(getObject()).withFailMessage("another message)").isNotEqualTo(null).as("a third message"); // Noncompliant
                                                                                                                                                     // [[sc=108;ec=120]]
    assertThat(new Object()).isEqualTo(null) // Noncompliant
      .isNotEqualTo(null); // Noncompliant

    assertThat(getBoolean()).isEqualTo(true); // Noncompliant {{Use isTrue() instead}}
    assertThat(getBoolean()).isEqualTo(false); // Noncompliant {{Use isFalse() instead}}
    assertThat(x.equals(y)).isTrue(); // Noncompliant [[sc=29;ec=35;secondary=70]] {{Use assertThat(actual).isEqualTo(expected) instead}}
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
    assertThat(x.compareTo(y)).isNotEqualTo(0).isNotEqualTo(-1); // Compliant as we have >1 context-dependant predicate
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

    assertThat(x.compareTo(y)).isOne(); // Compliant
  }

  // TODO: isNullOrEmpty
  void stringRelatedAssertionChains() {
    String x = "x";

    assertThat("some string").hasSize(0); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(x).isEqualTo(""); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(getString()).isNotEqualTo(""); // Compliant to avoid FP when getString could be null or a non-empty string
    assertThat(getString().equals(x)).isTrue(); // Noncompliant {{Replace with assertThat(actual).isEqualTo(expected)}}
    assertThat(getString().equals(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotEqualTo(expected)}}
    assertThat(getString().contentEquals(x)).isTrue(); // Noncompliant {{Replace with assertThat(actual).isEqualTo(expected)}}
    assertThat(getString().contentEquals(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotEqualTo(expected)}}
    assertThat(getString().equalsIgnoreCase(x)).isTrue(); // Noncompliant {{Replace with assertThat(actual).isEqualToIgnoringCase(expected)}}
    assertThat(getString().equalsIgnoreCase(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotEqualToIgnoringCase(expected)}}
    assertThat(getString().contains("some string")).isTrue(); // Noncompliant {{Replace with assertThat(actual).contains(expected)}}
    assertThat(getString().contains(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).doesNotContain(expected)}}
    assertThat(getString().startsWith(x)).isTrue(); // Noncompliant {{Replace with assertThat(actual).startsWith(expected)}}
    assertThat(getString().startsWith(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).doesNotStartWith(expected)}}
    assertThat(getString().endsWith(x)).isTrue(); // Noncompliant {{Replace with assertThat(actual).endsWith(expected)}}
    assertThat(getString().endsWith(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).doesNotEndWith(expected)}}
    assertThat(getString().matches(x)).isTrue(); // Noncompliant {{Replace with assertThat(actual).matches(expected)}}
    assertThat(getString().matches(x)).isFalse(); // Noncompliant {{Replace with assertThat(actual).doesNotMatch(expected)}}
    assertThat(getString().compareToIgnoreCase(x)).isEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isEqualToIgnoringCase(expected)}}
    assertThat(getString().compareToIgnoreCase(x)).isNotEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isNotEqualToIgnoringCase(expected)}}

    assertThat(getString().indexOf(x)).isEqualTo(0); // Noncompliant {{Replace with assertThat(actual).startsWith(expected)}}
    assertThat(getString().indexOf(x)).isNotEqualTo(0); // Noncompliant {{Replace with assertThat(actual).doesNotStartWith(expected)}}
    assertThat(getString().indexOf(x)).isZero(); // Noncompliant {{Replace with assertThat(actual).startsWith(expected)}}
    assertThat(getString().indexOf(x)).isNotZero(); // Noncompliant {{Replace with assertThat(actual).doesNotStartWith(expected)}}
    assertThat(getString().indexOf(x)).isEqualTo(-1); // Noncompliant {{Replace with assertThat(actual).doesNotContain(expected)}}
    assertThat(getString().indexOf(x)).isNegative(); // Noncompliant {{Replace with assertThat(actual).doesNotContain(expected)}}
    assertThat(getString().indexOf(x)).isLessThan(0); // Noncompliant {{Replace with assertThat(actual).doesNotContain(expected)}}
    assertThat(getString().indexOf(x)).isLessThanOrEqualTo(-1); // Noncompliant {{Replace with assertThat(actual).doesNotContain(expected)}}
    assertThat(getString().indexOf(x)).isNotNegative(); // Noncompliant {{Replace with assertThat(actual).contains(expected)}}
    assertThat(getString().indexOf(x)).isGreaterThanOrEqualTo(0); // Noncompliant {{Replace with assertThat(actual).contains(expected)}}
    assertThat(getString().indexOf(x)).isGreaterThan(-1); // Noncompliant {{Replace with assertThat(actual).contains(expected)}}

    assertThat(getString().trim()).isNotEmpty(); // Noncompliant {{Replace with assertThat(actual).isNotBlank()}}
    assertThat(getString().trim()).isNotEqualTo(""); // Noncompliant {{Replace with assertThat(actual).isNotBlank()}}
    assertThat(getString().length()).isEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(getString().length()).isLessThanOrEqualTo(0); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(getString().length()).isLessThan(-1); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(getString().length()).isZero(); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(getString().length()).isEqualTo(x); // Noncompliant {{Replace with assertThat(actual).hasSize(expected)}}
    assertThat(getString().isEmpty()).isTrue(); // Noncompliant {{Replace with assertThat(actual).isEmpty()}}
    assertThat(getString().isEmpty()).isFalse(); // Noncompliant {{Replace with assertThat(actual).isNotEmpty()}}
    assertThat(getString().length()).isEqualTo(x.length()); // Noncompliant {{Replace with assertThat(actual).hasSameSizeAs(expected)}}
  }
}
