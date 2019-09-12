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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;

public class JLabelSymbol implements Symbol.LabelSymbol, Symbol {

  private final String name;
  LabeledStatementTree declaration;
  final List<IdentifierTree> usages = new ArrayList<>();

  JLabelSymbol(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public LabeledStatementTree declaration() {
    return declaration;
  }

  @Override
  public List<IdentifierTree> usages() {
    return Collections.unmodifiableList(usages);
  }

  @Override
  public Symbol owner() {
    return null;
  }

  @Override
  public Type type() {
    return null;
  }

  @Override
  public boolean isVariableSymbol() {
    return false;
  }

  @Override
  public boolean isTypeSymbol() {
    return false;
  }

  @Override
  public boolean isMethodSymbol() {
    return false;
  }

  @Override
  public boolean isPackageSymbol() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPackageVisibility() {
    return false;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public SymbolMetadata metadata() {
    return new SymbolMetadata() {

      @Override
      public List<SymbolMetadata.AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
        return null;
      }

      @Override
      public boolean isAnnotatedWith(String fullyQualifiedName) {
        return false;
      }

      @Override
      public List<SymbolMetadata.AnnotationInstance> annotations() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public TypeSymbol enclosingClass() {
    return null;
  }

}
