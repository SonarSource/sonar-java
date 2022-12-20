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
package checks.tests;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

  private Long getLong() {
    return 42L;
  }

  private Map<Object, Object> getMap() {
    return new HashMap<>();
  }

  private HashMap<Object, Object> getHashMap() {
    return new HashMap<>();
  }

  private Object[] getArray() {
    return new Object[1];
  }

  private Collection<Object> getCollection() {
    return new ArrayList<>();
  }

  private List<Object> getList() {
    return new ArrayList<>();
  }

  private Set<Object> getSet() {
    return new HashSet<>();
  }

  private File getFile() {
    return File.listRoots()[0];
  }

  private Path getPath() {
    return null;
  }

  private Optional<Object> getOptional() {
    return null;
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
    assertThat(!getString().equals(x)).isTrue(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isNotEqualTo(expected) instead.}}
    assertThat(!getString().equals(x)).isFalse(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isEqualTo(expected) instead.}}
    assertThat(!!getString().equals(x)).isTrue(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isEqualTo(expected) instead.}}
    assertThat(!!getString().equals(x)).isFalse(); // compliant as boolean negation is unsupported but should be: {{Use assertThat(actual).isNotEqualTo(expected) instead.}}
  }

  void objectRelatedAssertionChains() {
    Comparable x = getBoolean();
    Object y = getObject();

    assertThat(getObject()).isEqualTo(null); // Noncompliant [[sc=29;ec=38]] {{Use isNull() instead.}}
    assertThat(getObject()).isNotEqualTo(null); // Noncompliant {{Use isNotNull() instead.}}
    assertThat(getObject())
      .isEqualTo(null) // Noncompliant
      .isNotEqualTo(getObject());
    assertThat(getObject()).as("some message)").isEqualTo(getObject()).withFailMessage("another message)").isNotEqualTo(null).as("a third message"); // Noncompliant [[sc=108;ec=120]]
    assertThat(new Object()).isEqualTo(null) // Noncompliant
      .isNotEqualTo(null); // Noncompliant

    assertThat(getBoolean()).isEqualTo(true); // Noncompliant {{Use isTrue() instead.}}
    assertThat(getObject()).isEqualTo(true); // Compliant
    assertThat(getBoolean()).isEqualTo(false); // Noncompliant {{Use isFalse() instead.}}
    assertThat(getObject()).isEqualTo(false); // Compliant
    assertThat(getObject()).isEqualTo(""); // Compliant
    assertThat(getObject()).isEqualTo(0); // Compliant
    assertThat(x.equals(y)).isTrue(); // Noncompliant [[sc=29;ec=35;secondary=+0]] {{Use assertThat(actual).isEqualTo(expected) instead.}}
    assertThat(x.equals(y)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualTo(expected) instead.}}
    assertThat(x == y).isTrue(); // Noncompliant {{Use assertThat(actual).isSameAs(expected) instead.}}
    assertThat(x == y).isFalse(); // Noncompliant {{Use assertThat(actual).isNotSameAs(expected) instead.}}
    assertThat(x != y).isTrue(); // Noncompliant {{Use assertThat(actual).isNotSameAs(expected) instead.}}
    assertThat(x != y).isFalse(); // Noncompliant {{Use assertThat(actual).isSameAs(expected) instead.}}
    assertThat(x == null).isTrue(); // Noncompliant {{Use assertThat(actual).isNull() instead.}}
    assertThat(x != null).isTrue(); // Noncompliant {{Use assertThat(actual).isNotNull() instead.}}
    assertThat(null == x).isFalse(); // Noncompliant {{Use assertThat(actual).isNotNull() instead.}}
    assertThat(x != null).isFalse(); // Noncompliant {{Use assertThat(actual).isNull() instead.}}
    assertThat(x.toString()).isEqualTo(y); // Noncompliant {{Use assertThat(actual).hasToString(expectedString) instead.}}
    assertThat(x.hashCode()).isEqualTo(y.hashCode()); // Noncompliant {{Use assertThat(actual).hasSameHashCodeAs(expected) instead.}}
    assertThat(getObject() instanceof String).isTrue(); // Noncompliant {{Use assertThat(actual).isInstanceOf(ExpectedClass.class) instead.}}
    assertThat(getObject() instanceof String).isFalse(); // Noncompliant {{Use assertThat(actual).isNotInstanceOf(ExpectedClass.class) instead.}}

    assertThat(x.compareTo(y)).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(x.compareTo(y)).isNotEqualTo(0); // Noncompliant {{Use isNotZero() instead.}}
    assertThat(getObject()).isNotEqualTo(0); // Compliant, can not use isNotZero on object
    assertThat(x.compareTo(y)).as("message").isNotEqualTo(0); // Noncompliant
    assertThat(x.compareTo(y)).isNotEqualTo(4).isNotEqualTo(null).isNotEqualTo(-1); // Noncompliant [[sc=48;ec=60]] - but only raise issue on the 'null' check

    assertThat(x.compareTo(y)).isZero(); // Noncompliant {{Use assertThat(actual).isEqualByComparingTo(expected) instead.}}
    assertThat(x.compareTo(y)).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEqualByComparingTo(expected) instead.}}
    assertThat(x.compareTo(y)).isGreaterThan(-1); // Noncompliant {{Use isNotNegative() instead.}}
    assertThat(x.compareTo(y)).isGreaterThan(0); // Noncompliant {{Use isPositive() instead.}}
    assertThat(x.compareTo(y)).isGreaterThanOrEqualTo(0); // Noncompliant {{Use isNotNegative() instead.}}
    assertThat(x.compareTo(y)).isGreaterThanOrEqualTo(1); // Noncompliant {{Use isPositive() instead.}}
    assertThat(x.compareTo(y)).isLessThan(1); // Noncompliant {{Use isNotPositive() instead.}}
    assertThat(x.compareTo(y)).isLessThan(0); // Noncompliant {{Use isNegative() instead.}}
    assertThat(x.compareTo(y)).isLessThanOrEqualTo(0); // Noncompliant {{Use isNotPositive() instead.}}
    assertThat(x.compareTo(y)).isLessThanOrEqualTo(-1); // Noncompliant {{Use isNegative() instead.}}
    assertThat(x.compareTo(y)).isNegative(); // Noncompliant {{Use assertThat(actual).isLessThan(expected) instead.}}
    assertThat(x.compareTo(y)).isNotNegative(); // Noncompliant {{Use assertThat(actual).isGreaterThanOrEqualTo(expected) instead.}}
    assertThat(x.compareTo(y)).isPositive(); // Noncompliant {{Use assertThat(actual).isGreaterThan(expected) instead.}}
    assertThat(x.compareTo(y)).isNotPositive(); // Noncompliant {{Use assertThat(actual).isLessThanOrEqualTo(expected) instead.}}
  }

  void test_equals_method() {
    Object obj = getObject();
    Object obj2 = getObject();
    assertThat(obj).isNotEqualTo(null); // Compliant, because the name of the test is related to "equals" and ".isNotNull()" does not call "obj.equals(null)"

    assertThat(obj.equals(obj2)).isFalse(); // Compliant, explicitly testing equals is acceptable in a method called equals.
    assertThat(obj.equals(obj)).isTrue(); // Compliant
    assertThat(obj.equals(Integer.valueOf(1))).isFalse(); // Compliant
    assertThat(obj.equals(null)).isFalse(); // Compliant
  }

  void relatedToComparable(int x, int y) {
    assertThat(x >= y).isTrue(); // Noncompliant {{Use assertThat(actual).isGreaterThanOrEqualTo(expected) instead.}}
    assertThat(x > y).isTrue(); // Noncompliant {{Use assertThat(actual).isGreaterThan(expected) instead.}}
    assertThat(x <= y).isTrue(); // Noncompliant {{Use assertThat(actual).isLessThanOrEqualTo(expected) instead.}}
    assertThat(x < y).isTrue(); // Noncompliant {{Use assertThat(actual).isLessThan(expected) instead.}}

    assertThat(x >= y).isFalse(); // Noncompliant {{Use assertThat(actual).isGreaterThanOrEqualTo(expected) instead.}}
    assertThat(x > y).isFalse(); // Noncompliant {{Use assertThat(actual).isGreaterThan(expected) instead.}}
    assertThat(x <= y).isFalse(); // Noncompliant {{Use assertThat(actual).isLessThanOrEqualTo(expected) instead.}}
    assertThat(x < y).isFalse(); // Noncompliant {{Use assertThat(actual).isLessThan(expected) instead.}}

    assertThat(x).isGreaterThanOrEqualTo(y); // Compliant
    assertThat(x).isGreaterThan(y); // Compliant
    assertThat(x).isLessThanOrEqualTo(y); // Compliant
    assertThat(x).isLessThan(y); // Compliant
  }

  void stringRelatedAssertionChains() {
    String x = "x";

    assertThat("some string").hasSize(0); // Noncompliant {{Use isEmpty() instead.}}
    assertThat(x).isEqualTo(""); // Noncompliant {{Use isEmpty() instead.}}
    assertThat(getString()).isNotEqualTo(""); // Compliant to avoid FP when getString could be null or a non-empty string
    assertThat(getString().equals(x)).isTrue(); // Noncompliant {{Use assertThat(actual).isEqualTo(expected) instead.}}
    assertThat(getString().equals(x)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualTo(expected) instead.}}
    assertThat(getString().contentEquals(x)).isTrue(); // Noncompliant {{Use assertThat(actual).isEqualTo(expected) instead.}}
    assertThat(getString().contentEquals(x)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualTo(expected) instead.}}
    assertThat(getString().equalsIgnoreCase(x)).isTrue(); // Noncompliant {{Use assertThat(actual).isEqualToIgnoringCase(expected) instead.}}
    assertThat(getString().equalsIgnoreCase(x)).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEqualToIgnoringCase(expected) instead.}}
    assertThat(getString().contains("some string")).isTrue(); // Noncompliant {{Use assertThat(actual).contains(expected) instead.}}
    assertThat(getString().contains(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead.}}
    assertThat(getString().startsWith(x)).isTrue(); // Noncompliant {{Use assertThat(actual).startsWith(expected) instead.}}
    assertThat(getString().startsWith(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotStartWith(expected) instead.}}
    assertThat(getString().endsWith(x)).isTrue(); // Noncompliant {{Use assertThat(actual).endsWith(expected) instead.}}
    assertThat(getString().endsWith(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotEndWith(expected) instead.}}
    assertThat(getString().matches(x)).isTrue(); // Noncompliant {{Use assertThat(actual).matches(expected) instead.}}
    assertThat(getString().matches(x)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotMatch(expected) instead.}}
    assertThat(getString().compareToIgnoreCase(x)).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getString().compareToIgnoreCase(x)).isNotEqualTo(0); // Noncompliant {{Use isNotZero() instead.}}
    assertThat(getString().compareToIgnoreCase(x)).isZero(); // Noncompliant {{Use assertThat(actual).isEqualToIgnoringCase(expected) instead.}}
    assertThat(getString().compareToIgnoreCase(x)).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEqualToIgnoringCase(expected) instead.}}

    assertThat(getString().indexOf(x)).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getString().indexOf(x)).isNotEqualTo(0); // Noncompliant {{Use isNotZero() instead.}}
    assertThat(getString().indexOf(x)).isZero(); // Noncompliant {{Use assertThat(actual).startsWith(expected) instead.}}
    assertThat(getString().indexOf(x)).isNotZero(); // Noncompliant {{Use assertThat(actual).doesNotStartWith(expected) instead.}}
    assertThat(getString().indexOf(x)).isEqualTo(-1); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead.}}
    assertThat(getString().indexOf(x)).isNegative(); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead.}}
    assertThat(getString().indexOf(x)).isLessThan(0); // Noncompliant {{Use isNegative() instead.}}
    assertThat(getString().indexOf(x)).isLessThanOrEqualTo(-1); // Noncompliant {{Use isNegative() instead.}}
    assertThat(getString().indexOf(x)).isNotNegative(); // Noncompliant {{Use assertThat(actual).contains(expected) instead.}}
    assertThat(getString().indexOf(x)).isGreaterThanOrEqualTo(0); // Noncompliant {{Use isNotNegative() instead.}}
    assertThat(getString().indexOf(x)).isGreaterThan(-1); // Noncompliant {{Use isNotNegative() instead.}}

    assertThat(getString().trim()).isNotEmpty(); // Noncompliant {{Use assertThat(actual).isNotBlank() instead.}}
    assertThat(getString().trim()).isNotEqualTo(""); // Noncompliant {{Use assertThat(actual).isNotBlank() instead.}}
    assertThat(getString().length()).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getString().length()).isEqualTo(12); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getString().length()).isEqualTo(x.length()); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead.}}
    assertThat(getString().length()).isEqualTo(getArray().length); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}

    assertThat(getString().length()).isLessThanOrEqualTo(0); // Noncompliant {{Use isNotPositive() instead.}}
    assertThat(getString().length()).isLessThan(1); // Noncompliant {{Use isNotPositive() instead.}}
    assertThat(getString().length()).isPositive(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getString().length()).isNotPositive(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getString().length()).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getString().length()).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getString().length()).isEqualTo(x); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getString().isEmpty()).isTrue(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getString().isEmpty()).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getString().isBlank()).isTrue(); // Noncompliant {{Use assertThat(actual).isBlank() instead.}}
    assertThat(getString().isBlank()).isFalse(); // Noncompliant {{Use assertThat(actual).isNotBlank() instead.}}
    assertThat(getString()).isBlank(); // Compliant
    assertThat(getString()).isNotBlank(); // Compliant
    assertThat(getString().length()).isEqualTo(x.length()); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead.}}
  }

  void arrayRelatedAssertionChains() {
    Object[] otherArray = new Object[1];
    int i = 12;
    MyClassWithLength myClassWithLength = new MyClassWithLength();

    assertThat(getArray().length).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getArray().length).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getArray().length).isEqualTo(i); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getArray().length).isPositive(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getArray().length).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getArray().length).isEqualTo(otherArray.length); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead.}}

    assertThat(getArray().length).isLessThanOrEqualTo(i); // Noncompliant {{Use assertThat(actual).hasSizeLessThanOrEqualTo(expected) instead.}}
    assertThat(getArray().length).isLessThan(i); // Noncompliant {{Use assertThat(actual).hasSizeLessThan(expected) instead.}}
    assertThat(getArray().length).isGreaterThan(i); // Noncompliant {{Use assertThat(actual).hasSizeGreaterThan(expected) instead.}}
    assertThat(getArray().length).isGreaterThanOrEqualTo(i); // Noncompliant {{Use assertThat(actual).hasSizeGreaterThanOrEqualTo(expected) instead.}}

    assertThat(getArray().length).isGreaterThan(0); // Noncompliant {{Use isPositive() instead.}}
    assertThat(myClassWithLength.length).isGreaterThanOrEqualTo(i); // Compliant
    assertThat(getArray().hashCode()).isPositive(); // Compliant
  }

  void collectionsRelatedAssertionChains() {
    int length = 42;
    String something = "";
    Collection<Object> otherCollection = new ArrayList<>();
    assertThat(getCollection()).hasSize(0); // Noncompliant {{Use isEmpty() instead.}}
    assertThat(getCollection().isEmpty()).isTrue(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getCollection().isEmpty()).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getCollection().size()).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}

    assertThat(getCollection().size()).isEqualTo(length); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getCollection().size()).isEqualTo(otherCollection.size()); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead.}}
    assertThat(getCollection().size()).isEqualTo(getArray().length); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getCollection().size()).isEqualTo(something.length()); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getCollection()).hasSameSizeAs(getArray()); // Compliant
    assertThat(getCollection().size()).isLessThanOrEqualTo(length); // Noncompliant {{Use assertThat(actual).hasSizeLessThanOrEqualTo(expected) instead.}}
    assertThat(getCollection().size()).isLessThan(length); // Noncompliant {{Use assertThat(actual).hasSizeLessThan(expected) instead.}}
    assertThat(getCollection().size()).isGreaterThan(length); // Noncompliant {{Use assertThat(actual).hasSizeGreaterThan(expected) instead.}}
    assertThat(getCollection().size()).isGreaterThanOrEqualTo(length); // Noncompliant {{Use assertThat(actual).hasSizeGreaterThanOrEqualTo(expected) instead.}}
    assertThat(getCollection().size()).isPositive(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}

    assertThat(getCollection().contains(something)).isTrue(); // Noncompliant {{Use assertThat(actual).contains(expected) instead.}}
    assertThat(getCollection().contains(something)).isFalse(); // Noncompliant {{Use assertThat(actual).doesNotContain(expected) instead.}}
    assertThat(getCollection().containsAll(otherCollection)).isTrue(); // Noncompliant {{Use assertThat(actual).containsAll(expected) instead.}}
    assertThat(getCollection().containsAll(otherCollection)).isFalse(); // Compliant, no method "doesNotContainsAll"

    // Same applies to Subtype of Collections.
    assertThat(getList().size()).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getList().size()).isEqualTo(length); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getList().contains(something)).isTrue(); // Noncompliant {{Use assertThat(actual).contains(expected) instead.}}
    assertThat(getList().containsAll(otherCollection)).isTrue(); // Noncompliant {{Use assertThat(actual).containsAll(expected) instead.}}

    assertThat(getSet().contains(something)).isTrue(); // Noncompliant {{Use assertThat(actual).contains(expected) instead.}}
  }

  void mapRelatedAssertionChains() {
    String value = "";
    String key = "";
    Map<Object, Object> otherMap = getMap();

    assertThat(getMap()).hasSize(0); // Noncompliant {{Use isEmpty() instead.}}
    assertThat(getMap().isEmpty()).isTrue(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getMap().isEmpty()).isFalse(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getMap().size()).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getMap().size()).isEqualTo(42); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getMap().size()).isEqualTo(otherMap.size()); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead.}}
    assertThat(getMap().size()).isEqualTo(getCollection().size()); // Noncompliant {{Use assertThat(actual).hasSameSizeAs(expected) instead.}}

    assertThat(getMap().size()).isLessThanOrEqualTo(42); // Noncompliant {{Use assertThat(actual).hasSizeLessThanOrEqualTo(expected) instead.}}
    assertThat(getMap().size()).isLessThan(42); // Noncompliant {{Use assertThat(actual).hasSizeLessThan(expected) instead.}}
    assertThat(getMap().size()).isGreaterThan(42); // Noncompliant {{Use assertThat(actual).hasSizeGreaterThan(expected) instead.}}
    assertThat(getMap().size()).isGreaterThanOrEqualTo(42); // Noncompliant {{Use assertThat(actual).hasSizeGreaterThanOrEqualTo(expected) instead.}}
    assertThat(getMap().size()).isLessThan(0); // Noncompliant {{Use isNegative() instead.}}
    assertThat(getMap().size()).isPositive(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}

    assertThat(getMap().containsKey(key)).isTrue(); // Noncompliant {{Use assertThat(actual).containsKey(expected) instead.}}
    assertThat(getMap().containsValue(value)).isTrue(); // Noncompliant {{Use assertThat(actual).containsValue(expected) instead.}}
    assertThat(getMap().keySet()).contains("foo"); // Noncompliant {{Use assertThat(actual).containsKey(expected) instead.}}
    assertThat(getMap().values()).contains("foo"); // Noncompliant {{Use assertThat(actual).containsValue(expected) instead.}}
    assertThat(getMap().keySet()).containsOnly("foo"); // Noncompliant {{Use assertThat(actual).containsOnlyKeys(expected) instead.}}
    assertThat(getMap().values()).containsOnly("foo"); // Compliant, no "containsOnlyValues"
    assertThat(getMap()).containsKey("foo"); // Compliant
    assertThat(getMap()).containsValue("foo"); // Compliant
    assertThat(getMap()).containsOnlyKeys("foo"); // Compliant

    assertThat(getMap().get(key)).isEqualTo(value); // Noncompliant {{Use assertThat(actual).containsEntry(key, value) instead.}}
    assertThat(getMap().get(key)).isNotEqualTo(value); // Noncompliant {{Use assertThat(actual).doesNotContainEntry(key, value) instead.}}

    // Same applies to subtypes of Map
    assertThat(getHashMap().isEmpty()).isTrue(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getHashMap().keySet()).contains("foo"); // Noncompliant {{Use assertThat(actual).containsKey(expected) instead.}}
    assertThat(getHashMap().get(key)).isEqualTo(value); // Noncompliant {{Use assertThat(actual).containsEntry(key, value) instead.}}
  }

  void fileRelatedAssertionChains() {
    int size = 1;
    String name = "name";

    assertThat(getFile()).hasSize(0); // Noncompliant {{Use isEmpty() instead.}}
    assertThat(getFile().length()).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getFile().length()).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getFile().length()).isNotZero(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}
    assertThat(getFile().length()).isNotEqualTo(0);	// Noncompliant {{Use isNotZero() instead.}}
    assertThat(getFile().length()).isEqualTo(size); // Noncompliant {{Use assertThat(actual).hasSize(expected) instead.}}
    assertThat(getFile().length()).isPositive(); // Noncompliant {{Use assertThat(actual).isNotEmpty() instead.}}

    assertThat(getFile().canRead()).isTrue(); // Noncompliant {{Use assertThat(actual).canRead() instead.}}
    assertThat(getFile().canWrite()).isTrue(); // Noncompliant {{Use assertThat(actual).canWrite() instead.}}
    assertThat(getFile().exists()).isTrue();// Noncompliant	{{Use assertThat(actual).exists() instead.}}
    assertThat(getFile().exists()).isFalse();	// Noncompliant {{Use assertThat(actual).doesNotExist() instead.}}
    assertThat(getFile().getName()).isEqualTo(name); // Noncompliant {{Use assertThat(actual).hasName(expected) instead.}}
    assertThat(getFile().getParent()).isEqualTo(name); // Noncompliant {{Use assertThat(actual).hasParent(expected) instead.}}
    assertThat(getFile().getParentFile()).isEqualTo(getFile()); // Noncompliant {{Use assertThat(actual).hasParent(expected) instead.}}
    assertThat(getFile().getParent()).isNull();	// Noncompliant {{Use assertThat(actual).hasNoParent() instead.}}
    assertThat(getFile().getParentFile()).isNull(); // Noncompliant	{{Use assertThat(actual).hasNoParent() instead.}}
    assertThat(getFile().isAbsolute()).isTrue(); // Noncompliant	{{Use assertThat(actual).isAbsolute() instead.}}
    assertThat(getFile().isAbsolute()).isFalse(); // Noncompliant	{{Use assertThat(actual).isRelative() instead.}}
    assertThat(getFile().isDirectory()).isTrue(); // Noncompliant	{{Use assertThat(actual).isDirectory() instead.}}
    assertThat(getFile().isFile()).isTrue(); // Noncompliant	{{Use assertThat(actual).isFile() instead.}}

    assertThat(getFile().list()).isEmpty(); // Noncompliant	{{Use assertThat(actual).isEmptyDirectory() instead.}}
    assertThat(getFile().listFiles()).isEmpty(); // Noncompliant	{{Use assertThat(actual).isEmptyDirectory() instead.}}
    assertThat(getFile().list()).isNotEmpty(); // Noncompliant	{{Use assertThat(actual).isNotEmptyDirectory() instead.}}
    assertThat(getFile().listFiles()).isNotEmpty(); // Noncompliant	{{Use assertThat(actual).isNotEmptyDirectory() instead.}}

    // We report only step by step, not the final transformation possible
    assertThat(getFile().list().length).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getFile().list().length).isZero(); // Noncompliant {{Use assertThat(actual).isEmpty() instead.}}
    assertThat(getFile().list()).isEmpty(); // Noncompliant	{{Use assertThat(actual).isEmptyDirectory() instead.}}
    assertThat(getFile()).isEmptyDirectory(); // Compliant, 3 iterations to reach a nice assertion
  }

  void pathRelatedAssertionChains() {
    String name = "path";

    assertThat(getPath().startsWith(name)).isTrue(); // Noncompliant	{{Use assertThat(actual).startsWithRaw(expected) instead.}}
    assertThat(getPath().endsWith(name)).isTrue(); // Noncompliant	{{Use assertThat(actual).endsWithRaw(expected) instead.}}
    assertThat(getPath().getParent()).isEqualTo(name); // Noncompliant	{{Use assertThat(actual).hasParentRaw(expected) instead.}}
    assertThat(getPath().getParent()).isNull();	// Noncompliant	{{Use assertThat(actual).hasNoParentRaw() instead.}}
    assertThat(getPath().isAbsolute()).isTrue(); // Noncompliant	{{Use assertThat(actual).isAbsolute() instead.}}
    assertThat(getPath().isAbsolute()).isFalse(); // Noncompliant	{{Use assertThat(actual).isRelative() instead.}}
  }

  void optionalRelatedAssertionChains() {
    assertThat(Optional.empty().isPresent()).isTrue(); // Noncompliant {{Use assertThat(actual).isPresent() instead.}}
    assertThat(getOptional().isPresent()).isFalse(); // Noncompliant {{Use assertThat(actual).isNotPresent() or assertThat(actual).isEmpty() instead.}}
    assertThat(getOptional().orElse(null)).isNull(); // Noncompliant {{Use assertThat(actual).isNotPresent() or assertThat(actual).isEmpty() instead.}}
    assertThat(getOptional().orElse(null)).isNotNull(); // Noncompliant {{Use assertThat(actual).isPresent() instead.}}
    assertThat(getOptional().orElse("foo")).isNotNull(); // Compliant
    assertThat(getOptional()).isEqualTo(Optional.empty()); // Noncompliant {{Use assertThat(actual).isNotPresent() or assertThat(actual).isEmpty() instead.}}
    assertThat(getOptional()).isNotEqualTo(Optional.empty()); // Noncompliant {{Use assertThat(actual).isPresent() instead.}}
    assertThat(getOptional().get()).isEqualTo(getObject()); // Noncompliant {{Use assertThat(actual).contains(expected) instead.}}
    assertThat(getOptional().get()).isSameAs(getObject()); // Noncompliant {{Use assertThat(actual).containsSame(expected) instead.}}
  }

  void contextFreeWithLong() {
    assertThat(getLong()).isEqualTo(0); // Noncompliant {{Use isZero() instead.}}
    assertThat(getLong()).isEqualTo(0L); // Noncompliant {{Use isZero() instead.}}
    assertThat(getLong()).isEqualTo(0l); // Noncompliant {{Use isZero() instead.}}
    assertThat(getLong()).isEqualTo(42l); // Compliant
    assertThat(getLong()).isEqualTo(42L); // Compliant

    assertThat(getLong()).isGreaterThan(0L); // Noncompliant {{Use isPositive() instead.}}
    assertThat(getLong()).isGreaterThan(-1L); // Noncompliant {{Use isNotNegative() instead.}}
    assertThat(getLong()).isGreaterThanOrEqualTo(0L); // Noncompliant {{Use isNotNegative() instead.}}
    assertThat(getLong()).isGreaterThanOrEqualTo(1L); // Noncompliant {{Use isPositive() instead.}}
    assertThat(getLong()).isGreaterThanOrEqualTo(1l); // Noncompliant {{Use isPositive() instead.}}
    assertThat(getLong()).isGreaterThanOrEqualTo(42L); // Compliant
    assertThat(getLong()).isLessThan(0L); // Noncompliant {{Use isNegative() instead.}}
    assertThat(getLong()).isLessThan(1L); // Noncompliant {{Use isNotPositive() instead.}}
    assertThat(getLong()).isLessThanOrEqualTo(0L);// Noncompliant {{Use isNotPositive() instead.}}
    assertThat(getLong()).isLessThanOrEqualTo(-1L);// Noncompliant {{Use isNegative() instead.}}
    assertThat(getLong()).isLessThanOrEqualTo(-42L);// Compliant
    assertThat(getLong()).isNotEqualTo(0L); // Noncompliant {{Use isNotZero() instead.}}
  }

  void compliantChains() {
    Comparable x = getBoolean();
    Object y = getObject();

    assertThat(x.compareTo(y)).isNotEqualTo(4).isNotEqualTo(5); // Compliant as we have >1 context-dependant predicate
    assertThat(x.compareTo(y)).isOne();
    assertThat(x).extracting("test").isEqualTo("test"); // Compliant as we ignore chains containing methods that change the assertion context (too complex)
    assertThat(true).isTrue();
    assertThat(getObject()).isNull();
    assertThat(x).isSameAs(y);
    assertThat(x).isLessThanOrEqualTo(6);
    assertThat(x.compareTo(y)).as("message"); // Compliant. Not a proper test, as the predicate is missing, but this is handled elsewhere.
    assertThat(x).isNotEqualByComparingTo(0);
    assertThat(x).hasToString("expected");
    assertThat(x).hasSameHashCodeAs(y);
    assertThat(x).isInstanceOf(Object.class);
    assertThat(getFile()).isAbsolute();
    assertThat(getFile()).canRead();
    assertThat(getOptional().orElse(new Object())).isNull();
    assertThat(getOptional().orElse(getObject())).isNotNull();
  }
}

class MyClassWithLength {
  int length = 2;
}
