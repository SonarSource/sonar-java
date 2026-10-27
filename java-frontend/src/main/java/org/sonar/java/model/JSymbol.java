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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * A symbol keyed by its SemSitter id. Two symbols with the same id are equal across files,
 * which is what a rule needs when the declaration and the use were translated separately.
 */
abstract class JSymbol implements Symbol {

  enum Kind {
    TYPE, METHOD, VARIABLE, PACKAGE
  }

  final JSema sema;
  final Kind kind;
  @Nullable
  final String id;
  private final String name;
  private Symbol owner = Symbol.UNKNOWN_SYMBOL;
  private Type type = Type.UNKNOWN;
  private boolean unknown;
  private boolean enumSymbol;
  private boolean interfaceSymbol;
  private boolean annotationSymbol;
  private boolean deprecated;
  private final Set<String> modifiers = new HashSet<>();
  private SymbolMetadata metadata;
  private int hash;

  JSymbol(JSema sema, Kind kind, @Nullable String id, String name) {
    this.sema = Objects.requireNonNull(sema);
    this.kind = kind;
    this.id = id;
    this.name = name == null ? "" : name;
  }

  void owner(Symbol owner) {
    this.owner = owner == null ? Symbol.UNKNOWN_SYMBOL : owner;
  }

  void type(Type type) {
    this.type = type == null ? Type.UNKNOWN : type;
  }

  void unknown(boolean unknown) {
    this.unknown = unknown;
  }

  void enumSymbol(boolean enumSymbol) {
    this.enumSymbol = enumSymbol;
  }

  void interfaceSymbol(boolean interfaceSymbol) {
    this.interfaceSymbol = interfaceSymbol;
  }

  void annotationSymbol(boolean annotationSymbol) {
    this.annotationSymbol = annotationSymbol;
  }

  boolean isAnnotationSymbol() {
    return annotationSymbol;
  }

  void deprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  void addModifiers(Collection<String> values) {
    for (String value : values) {
      for (String part : value.split(",")) {
        if (!part.isBlank()) {
          modifiers.add(part.trim());
        }
      }
    }
  }

  boolean hasModifier(String modifier) {
    return modifiers.contains(modifier);
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JSymbol other) || kind != other.kind) {
      return false;
    }
    if (id != null && other.id != null) {
      return id.equals(other.id);
    }
    return name.equals(other.name) && Objects.equals(owner, other.owner);
  }

  @Override
  public final int hashCode() {
    if (hash == 0) {
      hash = Objects.hash(kind, id != null ? id : name);
      if (hash == 0) {
        hash = 1;
      }
    }
    return hash;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final Symbol owner() {
    return owner;
  }

  @Override
  public Type type() {
    return type;
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
    return hasModifier("static");
  }

  @Override
  public final boolean isFinal() {
    return hasModifier("final");
  }

  @Override
  public final boolean isEnum() {
    return enumSymbol;
  }

  @Override
  public final boolean isInterface() {
    return interfaceSymbol;
  }

  @Override
  public final boolean isAbstract() {
    return hasModifier("abstract") || (interfaceSymbol && isMethodSymbol() && !hasModifier("default") && !hasModifier("static"));
  }

  @Override
  public final boolean isPublic() {
    return hasModifier("public") || interfaceSymbol;
  }

  @Override
  public final boolean isPrivate() {
    return hasModifier("private");
  }

  @Override
  public final boolean isProtected() {
    return hasModifier("protected");
  }

  @Override
  public final boolean isPackageVisibility() {
    return !isPublic() && !isPrivate() && !isProtected();
  }

  @Override
  public final boolean isDeprecated() {
    return deprecated;
  }

  @Override
  public final boolean isVolatile() {
    return hasModifier("volatile");
  }

  @Override
  public boolean isUnknown() {
    return unknown;
  }

  @Override
  public final SymbolMetadata metadata() {
    if (metadata == null) {
      metadata = new JSymbolMetadata(this);
    }
    return metadata;
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    if (this instanceof JTypeSymbol typeSymbol && typeSymbol.isTypeParameter()) {
      return owner() instanceof TypeSymbol ownerType ? ownerType : null;
    }
    Symbol current = this;
    while (current != null && !current.isTypeSymbol()) {
      if (current.isPackageSymbol() || current.isUnknown()) {
        return null;
      }
      current = current.owner();
    }
    return (TypeSymbol) current;
  }

  @Override
  public final List<IdentifierTree> usages() {
    if (id == null) {
      return Collections.emptyList();
    }
    List<IdentifierTree> found = sema.usages.get(id);
    return found == null ? Collections.emptyList() : found;
  }

  @Nullable
  @Override
  public Tree declaration() {
    return id == null ? null : sema.declarations.get(id);
  }

  @Override
  public String toString() {
    return id == null ? name : id;
  }
}
