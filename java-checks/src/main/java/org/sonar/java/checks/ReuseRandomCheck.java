/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2119")
public class ReuseRandomCheck extends AbstractMethodDetection {

  private static final Set<Kind> EXPRESSION_PARENT_CONTEXT = EnumSet.of(Kind.METHOD, Kind.CONSTRUCTOR, Kind.CLASS, Kind.ENUM, Kind.INTERFACE);

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create().typeDefinition("java.util.Random").name("<init>").withoutParameter());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (isInANonStaticMethod(newClassTree)) {
      reportIssue(newClassTree.identifier(), "Save and re-use this \"Random\".");
    }
  }

  private static boolean isInANonStaticMethod(NewClassTree newClassTree) {
    for (Tree parent = newClassTree.parent(); parent != null; parent = parent.parent()) {
      if (isNonStaticMethod(parent)) {
        return true;
      } else if (EXPRESSION_PARENT_CONTEXT.contains(parent.kind())) {
        break;
      }
    }
    return false;
  }

  private static boolean isNonStaticMethod(Tree tree) {
    return tree.is(Kind.METHOD) && !ModifiersUtils.hasModifier(((MethodTree)tree).modifiers(), Modifier.STATIC);
  }

}
