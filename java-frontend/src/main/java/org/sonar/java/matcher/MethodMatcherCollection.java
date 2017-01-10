/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.Lists;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MethodMatcherCollection {

  private List<MethodMatcher> matchers = Lists.newLinkedList();

  private MethodMatcherCollection() {
  }

  public static MethodMatcherCollection create(MethodMatcher... matchers) {
    MethodMatcherCollection collection = new MethodMatcherCollection();
    Collections.addAll(collection.matchers, matchers);
    return collection;
  }

  public MethodMatcherCollection add(MethodMatcher matcher) {
    this.matchers.add(matcher);
    return this;
  }

  public MethodMatcherCollection addAll(Collection<MethodMatcher> matchers) {
    this.matchers.addAll(matchers);
    return this;
  }

  public boolean anyMatch(MethodInvocationTree mit) {
    for (MethodMatcher matcher : matchers) {
      if (matcher.matches(mit)) {
        return true;
      }
    }
    return false;
  }

  public boolean anyMatch(final MethodTree method) {
    for (MethodMatcher matcher : matchers) {
      if (matcher.matches(method)) {
        return true;
      }
    }
    return false;
  }

  public boolean anyMatch(NewClassTree newClassTree) {
    for (MethodMatcher matcher : matchers) {
      if (matcher.matches(newClassTree)) {
        return true;
      }
    }
    return false;
  }
}
