/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.resolve;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeSubstitutionTest {

  TypeSubstitution substitution;
  TypeVariableJavaType k;
  TypeVariableJavaType v;
  JavaType c1;
  JavaType c2;
  JavaSymbol.PackageJavaSymbol packageJavaSymbol = new JavaSymbol.PackageJavaSymbol(null, null);

  @Before
  public void setUp() {
    k = new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol("K", packageJavaSymbol));
    v = new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol("V", packageJavaSymbol));
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

  @Test
  public void test_combine() throws Exception {
    TypeVariableJavaType a = newTypeVar("A");
    TypeVariableJavaType b = newTypeVar("B");
    TypeVariableJavaType x = newTypeVar("X");
    TypeVariableJavaType y = newTypeVar("Y");
    JavaType s = newType("S");
    JavaType i = newType("I");
    TypeSubstitution t0 = new TypeSubstitution().add(a, s).add(b, i);
    TypeSubstitution t1 = new TypeSubstitution().add(a, x).add(b, y);

    TypeSubstitution combined = t1.combine(t0);
    assertThat(combined.typeVariables()).hasSize(2).containsSequence(x, y);
    assertThat(combined.substitutedTypes()).hasSize(2).containsSequence(s, i);

    TypeSubstitution t3 = new TypeSubstitution().add(a, s).add(b, newParameterizedType("G", i));
    TypeSubstitution t4 = new TypeSubstitution().add(a, x).add(b, newParameterizedType("G", y));
    combined = t4.combine(t3);
    assertThat(combined.typeVariables()).hasSize(2).containsSequence(x, y);
    assertThat(combined.substitutedTypes()).hasSize(2).containsSequence(s, i);

    TypeSubstitution t5 = new TypeSubstitution().add(a, new WildCardType(x, WildCardType.BoundType.SUPER)).add(b, new WildCardType(y, WildCardType.BoundType.EXTENDS));
    combined = t5.combine(t0);
    assertThat(combined.typeVariables()).hasSize(2).containsSequence(x, y);
    assertThat(combined.substitutedTypes()).hasSize(2).containsSequence(s, i);


    TypeSubstitution t6 = new TypeSubstitution().add(a, s).add(b, new ArrayJavaType(i, null));
    TypeSubstitution t7 = new TypeSubstitution().add(a, x).add(b, new ArrayJavaType(y, null));
    combined = t7.combine(t6);
    assertThat(combined.typeVariables()).hasSize(2).containsSequence(x, y);
    assertThat(combined.substitutedTypes()).hasSize(2).containsSequence(s, i);

    ParametrizedTypeJavaType listOfY = newParameterizedType("List", y);
    TypeSubstitution t8 = new TypeSubstitution().add(a, new WildCardType(x, WildCardType.BoundType.SUPER)).add(b, listOfY);
    TypeSubstitution t9 = new TypeSubstitution().add(a, y).add(b, listOfY);
    combined = t8.combine(t9);
    assertThat(combined.typeVariables()).hasSize(2).containsSequence(x, b);
    assertThat(combined.substitutedTypes()).hasSize(2).containsSequence(y, listOfY);
  }

  private JavaType newType(String name) {
    return new JavaType(JavaType.CLASS, new JavaSymbol.TypeJavaSymbol(0, name, packageJavaSymbol));
  }

  private TypeVariableJavaType newTypeVar(String name) {
    return new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol(name, packageJavaSymbol));
  }

  private ParametrizedTypeJavaType newParameterizedType(String name, JavaType substitutedType) {
    JavaSymbol.TypeJavaSymbol symbol = new JavaSymbol.TypeJavaSymbol(0, name, packageJavaSymbol);
    symbol.addTypeParameter(k);
    TypeSubstitution newSubstitution = new TypeSubstitution().add(k, substitutedType);
    return new ParametrizedTypeJavaType(symbol, newSubstitution, null);
  }

}
