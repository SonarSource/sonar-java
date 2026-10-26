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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

final class JMethodSymbol extends JSymbol implements Symbol.MethodSymbol {

  JMethodSymbol(JSema sema) {
    super(sema, Kind.METHOD);
  }

  public boolean isLambda() {
    return false;
  }

  @Override
  public List<Type> parameterTypes() {
    return Collections.emptyList();
  }

  @Override
  public List<Symbol> declarationParameters() {
    return Collections.emptyList();
  }

  @Override
  public TypeSymbol returnType() {
    return TypeSymbol.UNKNOWN_TYPE;
  }

  @Override
  public List<Type> thrownTypes() {
    return Collections.emptyList();
  }

  @Override
  public List<MethodSymbol> overriddenSymbols() {
    return Collections.emptyList();
  }

  @Override
  public String signature() {
    return "";
  }

  @Override
  public boolean isOverridable() {
    return false;
  }

  @Override
  public boolean isParametrizedMethod() {
    return false;
  }

  @Override
  public boolean isDefaultMethod() {
    return false;
  }

  @Override
  public boolean isSynchronizedMethod() {
    return false;
  }

  @Override
  public boolean isVarArgsMethod() {
    return false;
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return null;
  }

  @Override
  public boolean isNativeMethod() {
    return false;
  }

}
