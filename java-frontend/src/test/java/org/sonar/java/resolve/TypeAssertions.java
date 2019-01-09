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

import org.assertj.core.api.AbstractAssert;
import org.sonar.plugins.java.api.semantic.Type;

public class TypeAssertions extends AbstractAssert<TypeAssertions, Type> {

  public TypeAssertions(Type actual) {
    super(actual, TypeAssertions.class);
  }

  public TypeAssertions isSubtypeOf(String fullyQualifiedName) {
    if (!actual.isSubtypeOf(fullyQualifiedName)) {
      failWithMessage("Expecting '%s' to be subtype of '%s'", actual.name(), fullyQualifiedName);
    }
    return this;
  }

  public TypeAssertions isNotSubtypeOf(String fullyQualifiedName) {
    if (actual.isSubtypeOf(fullyQualifiedName)) {
      failWithMessage("Expecting '%s' to NOT be subtype of '%s'", actual.name(), fullyQualifiedName);
    }
    return this;
  }

  public TypeAssertions is(String fullyQualifiedName) {
    if (!actual.is(fullyQualifiedName)) {
      failWithMessage("Expecting '%s' to be of type '%s'", actual.name(), fullyQualifiedName);
    }
    return this;
  }

  public TypeAssertions isNot(String fullyQualifiedName) {
    if (actual.is(fullyQualifiedName)) {
      failWithMessage("Expecting '%s' to NOT be of type '%s'", actual.name(), fullyQualifiedName);
    }
    return this;
  }

  public static TypeAssertions assertThat(Type actual) {
    return new TypeAssertions(actual);
  }

  public TypeAssertions isUnknown() {
    if (!actual.isUnknown()) {
      failWithMessage("Expecting '%s' to be unknown", actual.name());
    }
    return this;
  }

}
