/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.matcher;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

public class MethodMatchersList implements MethodMatchers {

  private List<? extends MethodMatchers> matchers;

  public MethodMatchersList(List<? extends MethodMatchers> matchers) {
    this.matchers = new ArrayList<>(matchers);
  }

  @Override
  public boolean matches(NewClassTree newClassTree) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(newClassTree));
  }

  @Override
  public boolean matches(MethodInvocationTree mit) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(mit));
  }

  @Override
  public boolean matches(MethodTree methodTree) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(methodTree));
  }

  @Override
  public boolean matches(MethodReferenceTree methodReferenceTree) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(methodReferenceTree));
  }

  @Override
  public boolean matches(Symbol symbol) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(symbol));
  }

}
