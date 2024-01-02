/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4065")
public class ThreadLocalWithInitialCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final MethodMatchers THREADLOCAL_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("java.lang.ThreadLocal")
    .constructor()
    .addWithoutParametersMatcher()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return THREADLOCAL_CONSTRUCTOR;
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
      .map(MethodTree.class::cast)
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
