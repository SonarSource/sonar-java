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

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;

final class JTypeSymbol extends JSymbol implements Symbol.TypeSymbol {

  private final boolean typeParameter;
  private int typeParameterCount;
  @Nullable
  private String superName;
  private List<String> interfaceNames = List.of();
  private List<JsonObject> memberFacts = List.of();
  @Nullable
  private String elementFqn;
  private Collection<Symbol> members;

  JTypeSymbol(JSema sema, @Nullable String id, String name, boolean typeParameter) {
    super(sema, Kind.TYPE, id, name);
    this.typeParameter = typeParameter;
  }

  boolean isTypeParameter() {
    return typeParameter;
  }

  int typeParameterCount() {
    return typeParameterCount;
  }

  void typeParameterCount(int typeParameterCount) {
    this.typeParameterCount = typeParameterCount;
  }

  void superName(@Nullable String superName) {
    this.superName = superName;
  }

  void interfaceNames(List<String> interfaceNames) {
    this.interfaceNames = interfaceNames;
  }

  void memberFacts(List<JsonObject> memberFacts) {
    this.memberFacts = memberFacts;
    this.members = null;
  }

  List<JsonObject> memberFacts() {
    return memberFacts;
  }

  void elementFqn(@Nullable String elementFqn) {
    this.elementFqn = elementFqn;
  }

  @CheckForNull
  @Override
  public Type superClass() {
    if (typeParameter || elementFqn != null) {
      return sema.getClassType("java.lang.Object");
    }
    if (isInterface() || isAnnotation()) {
      return sema.getClassType("java.lang.Object");
    }
    String erased = JSema.erase(id == null ? name() : id);
    if ("java.lang.Object".equals(erased)) {
      return null;
    }
    if (superName == null || superName.isEmpty() || inheritsFrom(erased)) {
      return sema.getClassType("java.lang.Object");
    }
    return sema.getClassType(JSema.erase(superName));
  }

  private boolean inheritsFrom(String erasedSelf) {
    Set<String> visited = new HashSet<>();
    String current = JSema.erase(superName);
    while (visited.add(current)) {
      if (current.equals(erasedSelf)) {
        return true;
      }
      if (!(sema.getClassType(current).symbol() instanceof JTypeSymbol parent) || parent.superName == null || parent.superName.isEmpty()) {
        return false;
      }
      current = JSema.erase(parent.superName);
    }
    return false;
  }

  @Override
  public List<Type> interfaces() {
    if (elementFqn != null) {
      return List.of(sema.getClassType("java.lang.Cloneable"), sema.getClassType("java.io.Serializable"));
    }
    if (interfaceNames.isEmpty()) {
      return Collections.emptyList();
    }
    List<Type> types = new ArrayList<>();
    for (String name : interfaceNames) {
      types.add(sema.getClassType(JSema.erase(name)));
    }
    return types;
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    if (members == null) {
      List<Symbol> resolved = new ArrayList<>();
      for (JsonObject member : memberFacts) {
        resolved.add(sema.memberFromTypeInfo(this, member));
      }
      members = resolved;
    }
    return members;
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    Set<Symbol> found = new HashSet<>();
    for (Symbol member : memberSymbols()) {
      if (name.equals(member.name())) {
        found.add(member);
      }
    }
    return found;
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return super.declaration() instanceof ClassTree classTree ? classTree : null;
  }

  @Override
  public Set<Type> superTypes() {
    if (isUnknown()) {
      return Collections.emptySet();
    }
    Set<Type> collected = new HashSet<>();
    collectSuperTypes(collected, new HashSet<>());
    return collected;
  }

  private void collectSuperTypes(Set<Type> collected, Set<String> visited) {
    if (!visited.add(JSema.erase(id == null ? name() : id))) {
      return;
    }
    Type superClass = superClass();
    if (superClass != null && !superClass.isUnknown()) {
      collected.add(superClass);
      if (superClass.symbol() instanceof JTypeSymbol parent) {
        parent.collectSuperTypes(collected, visited);
      }
    }
    for (Type iface : interfaces()) {
      if (!iface.isUnknown()) {
        collected.add(iface);
        if (iface.symbol() instanceof JTypeSymbol parent) {
          parent.collectSuperTypes(collected, visited);
        }
      }
    }
  }

  @Nullable
  @Override
  public TypeSymbol outermostClass() {
    Symbol current = this;
    Symbol result = this;
    while (current != null && !current.isPackageSymbol()) {
      result = current;
      current = current.owner();
    }
    return result instanceof TypeSymbol typeSymbol ? typeSymbol : this;
  }

  @Override
  public boolean isAnnotation() {
    return !isUnknown() && isAnnotationSymbol();
  }
}
