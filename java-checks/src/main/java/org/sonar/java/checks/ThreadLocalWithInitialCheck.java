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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S4065")
public class ThreadLocalWithInitialCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {
  private static final MethodMatcher THREADLOCAL_CONSTRUCTOR = MethodMatcher.create().typeDefinition("java.lang.ThreadLocal").name("<init>").withoutParameter();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(THREADLOCAL_CONSTRUCTOR);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    ClassTree classTree = newClassTree.classBody();
    if (classTree == null) {
      return;
    }
    List<Tree> members = classTree.members();
    if (members.size() != 1) {
      return;
    }
    members.stream()
      .filter(tree -> tree.is(Tree.Kind.METHOD))
      .map(t -> ((MethodTree) t))
      .filter(t -> "initialValue".equals(t.simpleName().name()))
      .filter(t -> t.parameters().isEmpty())
      .findFirst().ifPresent(
        t -> reportIssue(newClassTree.identifier(), "Replace this anonymous class with a call to \"ThreadLocal.withInitial\"."+context.getJavaVersion().java8CompatibilityMessage())
      );
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }
}
