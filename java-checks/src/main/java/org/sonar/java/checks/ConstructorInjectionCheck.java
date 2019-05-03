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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Rule(key = "S3306")
public class ConstructorInjectionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    List<Tree> members = ((ClassTree) tree).members();
    Optional<Tree> first = members.stream().filter(t -> t.is(Tree.Kind.CONSTRUCTOR) && isPrivateConstructor((MethodTree) t)).findFirst();
    if(first.isPresent()) {
      return;
    }
    members.stream()
      .filter(t -> t.is(Tree.Kind.VARIABLE) && isAnnotatedWithInject((VariableTree) t))
      .forEach(field -> reportIssue(((VariableTree) field).simpleName(), "Use constructor injection for this field.")
    );
  }

  private static boolean isPrivateConstructor(MethodTree constructor) {
    return constructor.symbol().isPrivate();
  }

  private static boolean isAnnotatedWithInject(VariableTree field) {
    return field.symbol().metadata().isAnnotatedWith("javax.inject.Inject");
  }

}
