/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.semantic.Type;

final class JUnionType extends JType {

  private final Type[] unionTypes;
  private final String unionName;
  private final String unionFullyQualifiedName;

  JUnionType(JSema sema, ITypeBinding typeBinding, ITypeBinding[] alternativeBindings) {
    super(sema, typeBinding);
    unionTypes = unionTypes(sema, alternativeBindings);
    unionName = unionName(unionTypes);
    unionFullyQualifiedName = unionFullyQualifiedName(unionTypes);
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    return unionFullyQualifiedName.equals(fullyQualifiedName)
      || fullyQualifiedName.equals(sema.type(typeBinding).fullyQualifiedName());
  }

  @Override
  public String fullyQualifiedName() {
    return unionFullyQualifiedName;
  }

  @Override
  public String name() {
    return unionName;
  }

  @Override
  public String toString() {
    return unionName;
  }

  @Override
  public boolean isUnionType() {
    return true;
  }

  @Override
  public Type[] getUnionTypes() {
    return unionTypes.clone();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof JUnionType other && unionFullyQualifiedName.equals(other.unionFullyQualifiedName));
  }

  @Override
  public int hashCode() {
    return unionFullyQualifiedName.hashCode();
  }

  private static Type[] unionTypes(JSema sema, ITypeBinding[] alternativeBindings) {
    Type[] result = new Type[alternativeBindings.length];
    for (int i = 0; i < alternativeBindings.length; i++) {
      ITypeBinding alternativeBinding = alternativeBindings[i];
      result[i] = alternativeBinding == null ? Type.UNKNOWN : sema.type(alternativeBinding);
    }
    return result;
  }

  private static String unionFullyQualifiedName(Type[] unionTypes) {
    return Arrays.stream(unionTypes)
      .map(Type::fullyQualifiedName)
      .distinct()
      .sorted()
      .collect(Collectors.joining(" | "));
  }

  private static String unionName(Type[] unionTypes) {
    return Arrays.stream(unionTypes)
      .map(Type::name)
      .sorted()
      .collect(Collectors.joining(" | "));
  }
}
