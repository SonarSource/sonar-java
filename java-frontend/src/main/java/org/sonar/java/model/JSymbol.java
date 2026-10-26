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
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

abstract class JSymbol implements Symbol {

  enum Kind {
    TYPE,
    METHOD,
    VARIABLE,
    PACKAGE
  }

  protected final JSema sema;
  private final Kind kind;

  JSymbol(JSema sema, Kind kind) {
    this.sema = Objects.requireNonNull(sema);
    this.kind = kind;
  }

  @Override
  public final String name() {
    return "";
  }

  @Override
  public final Symbol owner() {
    return kind == Kind.PACKAGE ? Symbol.ROOT_PACKAGE : Symbol.UNKNOWN_SYMBOL;
  }

  @Override
  public final Type type() {
    return Type.UNKNOWN;
  }

  @Override
  public final boolean isVariableSymbol() {
    return kind == Kind.VARIABLE;
  }

  @Override
  public final boolean isTypeSymbol() {
    return kind == Kind.TYPE;
  }

  @Override
  public final boolean isMethodSymbol() {
    return kind == Kind.METHOD;
  }

  @Override
  public final boolean isPackageSymbol() {
    return kind == Kind.PACKAGE;
  }

  @Override
  public final boolean isStatic() {
    return false;
  }

  @Override
  public final boolean isFinal() {
    return false;
  }

  @Override
  public final boolean isEnum() {
    return false;
  }

  @Override
  public final boolean isInterface() {
    return false;
  }

  @Override
  public final boolean isAbstract() {
    return false;
  }

  @Override
  public final boolean isPublic() {
    return false;
  }

  @Override
  public final boolean isPrivate() {
    return false;
  }

  @Override
  public final boolean isProtected() {
    return false;
  }

  @Override
  public final boolean isPackageVisibility() {
    return false;
  }

  @Override
  public final boolean isDeprecated() {
    return false;
  }

  @Override
  public final boolean isVolatile() {
    return false;
  }

  @Override
  public final boolean isUnknown() {
    return true;
  }

  @Override
  public final SymbolMetadata metadata() {
    return Symbols.EMPTY_METADATA;
  }

  @Nullable
  @Override
  public final TypeSymbol enclosingClass() {
    return null;
  }

  @Override
  public final List<IdentifierTree> usages() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public Tree declaration() {
    return null;
  }

}
