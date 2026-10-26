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
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;

final class JTypeSymbol extends JSymbol implements Symbol.TypeSymbol {

  JTypeSymbol(JSema sema) {
    super(sema, Kind.TYPE);
  }

  @CheckForNull
  @Override
  public Type superClass() {
    return null;
  }

  @Override
  public List<Type> interfaces() {
    return Collections.emptyList();
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    return Collections.emptyList();
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return null;
  }

  @Override
  public Set<Type> superTypes() {
    return Collections.emptySet();
  }

  @Nullable
  @Override
  public TypeSymbol outermostClass() {
    return this;
  }

  @Override
  public boolean isAnnotation() {
    return false;
  }

}
