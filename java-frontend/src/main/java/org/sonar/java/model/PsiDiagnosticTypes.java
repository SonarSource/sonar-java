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

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;

final class PsiDiagnosticTypes {
  private PsiDiagnosticTypes() {
  }

  static String name(com.intellij.psi.PsiType type) {
    if (type instanceof PsiArrayType array) {
      return name(array.getComponentType()) + "[]";
    }
    if (type instanceof PsiClassType cls && cls.resolve() != null) {
      var declaration = cls.resolve();
      String name = declaration.getName();
      for (var owner = declaration instanceof com.intellij.psi.PsiTypeParameter ? null : declaration.getContainingClass(); owner != null; owner = owner.getContainingClass()) {
        name = owner.getName() + "." + name;
      }
      if (cls.getParameters().length != 0) {
        name += java.util.Arrays.stream(cls.getParameters()).map(PsiDiagnosticTypes::name).collect(java.util.stream.Collectors.joining(",", "<", ">"));
      }
      return name;
    }
    if (type instanceof com.intellij.psi.PsiWildcardType wildcard) {
      return wildcard.isBounded() ? "? " + (wildcard.isExtends() ? "extends " : "super ") + name(wildcard.getBound()) : "?";
    }
    return type.getPresentableText();
  }

  static boolean mentions(com.intellij.psi.PsiType type, com.intellij.psi.PsiTypeParameter parameter) {
    if (type instanceof PsiClassType cls) {
      return parameter.equals(cls.resolve()) || java.util.Arrays.stream(cls.getParameters()).anyMatch(argument -> mentions(argument, parameter));
    }
    if (type instanceof PsiArrayType array) {
      return mentions(array.getComponentType(), parameter);
    }
    if (type instanceof com.intellij.psi.PsiWildcardType wildcard && wildcard.isBounded()) {
      return mentions(wildcard.getBound(), parameter);
    }
    if (type instanceof com.intellij.psi.PsiIntersectionType intersection) {
      return java.util.Arrays.stream(intersection.getConjuncts()).anyMatch(conjunct -> mentions(conjunct, parameter));
    }
    return false;
  }
}
