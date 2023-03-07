/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Type;

public class TypeAssert extends AbstractAssert<TypeAssert, Type> {

  public TypeAssert(Type actual) {
    super(actual, TypeAssert.class);
  }

  public static TypeAssert assertThat(Type actual) {
    return new TypeAssert(actual);
  }

  public TypeAssert hasName(String name) {
    Assertions.assertThat(actual.name())
      .as(descriptionText())
      .isEqualTo(name);
    return this;
  }

  public TypeAssert hasSameNameAs(Type other) {
    return hasName(other.name());
  }

  public TypeAssert is(String fullyQualifiedName) {
    Assertions.assertThat(actual.fullyQualifiedName())
      .as(descriptionText())
      .isEqualTo(fullyQualifiedName);
    return this;
  }

  public TypeAssert is(Type type) {
    Assertions.assertThat(actual)
      .as(descriptionText())
      .isSameAs(type);
    return this;
  }

  public TypeAssert isSubtypeOf(Type superType) {
    Assertions.assertThat(actual.isSubtypeOf(superType))
      .as(descriptionText())
      .isTrue();
    return this;
  }

  public TypeAssert isSubtypeOf(String superTypeFullyQualifiedName) {
    Assertions.assertThat(actual.isSubtypeOf(superTypeFullyQualifiedName))
      .as(descriptionText())
      .isTrue();
    return this;
  }

  public TypeAssert isNotSubtypeOf(Type superType) {
    Assertions.assertThat(actual.isSubtypeOf(superType))
      .as(descriptionText())
      .isFalse();
    return this;
  }

  public TypeAssert isUnknown() {
    Assertions.assertThat(actual.isUnknown())
      .as(descriptionText())
      .isTrue();
    Assertions.assertThat(actual)
      .as(descriptionText())
      .isSameAs(Symbols.unknownType);
    return this;
  }


}
