/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TypeSubstitutionTest {

  TypeSubstitution substitution;
  JavaType.TypeVariableJavaType k;
  JavaType.TypeVariableJavaType v;
  JavaType c1;
  JavaType c2;

  @Before
  public void setUp() {
    k = new JavaType.TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol("K", null));
    v = new JavaType.TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol("V", null));
    c1 = new JavaType(JavaType.CLASS, null);
    c2 = new JavaType(JavaType.CLASS, null);

    substitution = new TypeSubstitution()
      .add(k, c1)
      .add(v, c2);
  }

  @Test
  public void should_be_empty() {
    TypeSubstitution substitution = new TypeSubstitution();
    assertThat(substitution.size()).isEqualTo(0);
    assertThat(substitution.substitutionEntries()).isEmpty();
    assertThat(substitution.substitutedTypes()).isEmpty();
    assertThat(substitution.typeVariables()).isEmpty();
  }

  @Test
  public void should_contain_exact_number_of_substitution() {
    assertThat(substitution.size()).isEqualTo(2);
    assertThat(substitution.substitutionEntries()).hasSize(2);
    assertThat(substitution.substitutedTypes()).hasSize(2);
    assertThat(substitution.typeVariables()).hasSize(2);
  }

  @Test
  public void should_contain_substitution() {
    assertThat(substitution.substitutedType(k)).isEqualTo(c1);
    assertThat(substitution.substitutedType(v)).isEqualTo(c2);
  }

  @Test
  public void should_be_ordered() {
    assertThat(substitution.typeVariables().get(0)).isEqualTo(k);
    assertThat(substitution.typeVariables().get(1)).isEqualTo(v);

    assertThat(substitution.substitutedTypes().get(0)).isEqualTo(c1);
    assertThat(substitution.substitutedTypes().get(1)).isEqualTo(c2);
  }

  @Test
  public void equivalent_type_substitutions_should_compute_same_hashCode() {
    TypeSubstitution newSubstitution = new TypeSubstitution()
      .add(k, c1)
      .add(v, c2);

    assertThat(substitution.hashCode()).isEqualTo(newSubstitution.hashCode());
  }

  @Test
  public void order_should_be_Taken_into_account_for_hashCode() {
    // couples swapped
    TypeSubstitution newSubstitution = new TypeSubstitution()
      .add(v, c2)
      .add(k, c1);
    assertThat(substitution.hashCode()).isNotEqualTo(newSubstitution.hashCode());

    // variables swapped
    newSubstitution = new TypeSubstitution()
      .add(v, c1)
      .add(k, c2);
    assertThat(substitution.hashCode()).isNotEqualTo(newSubstitution.hashCode());

    // substitutions swapped
    newSubstitution = new TypeSubstitution()
      .add(k, c2)
      .add(v, c1);
    assertThat(substitution.hashCode()).isNotEqualTo(newSubstitution.hashCode());
  }

  @Test
  public void equivalent_type_substitutions_should_be_equals() {
    TypeSubstitution newSubstitution = new TypeSubstitution()
      .add(k, c1)
      .add(v, c2);

    assertThat(substitution.equals(newSubstitution)).isTrue();
    assertThat(substitution.equals(substitution)).isTrue();
  }

  @Test
  public void different_substitution_are_not_equals() {
    assertThat(substitution.equals(new TypeSubstitution())).isFalse();
    assertThat(substitution.equals(null)).isFalse();
    assertThat(substitution.equals(new Object())).isFalse();
  }

  @Test
  public void order_should_be_taken_into_account_for_equality_test() {
    // couples swapped
    TypeSubstitution newSubstitution = new TypeSubstitution()
      .add(v, c2)
      .add(k, c1);
    assertThat(substitution.equals(newSubstitution)).isFalse();

    // variables swapped
    newSubstitution = new TypeSubstitution()
      .add(v, c1)
      .add(k, c2);
    assertThat(substitution.equals(newSubstitution)).isFalse();

    // substitutions swapped
    newSubstitution = new TypeSubstitution()
      .add(k, c2)
      .add(v, c1);
    assertThat(substitution.equals(newSubstitution)).isFalse();
  }
}
