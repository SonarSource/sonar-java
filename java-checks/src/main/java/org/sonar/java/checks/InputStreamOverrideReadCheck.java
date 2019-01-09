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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4929")
public class InputStreamOverrideReadCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher READ_BYTES_INT_INT = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("read").parameters("byte[]", "int", "int");
  private static final MethodMatcher READ_INT = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("read").parameters("int");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ClassTree classTree = (ClassTree) tree;
    Type superType = classTree.symbol().superClass();
    IdentifierTree className = classTree.simpleName();
    if (className == null || classTree.symbol().isAbstract() || superType == null || !(superType.is("java.io.InputStream") || superType.is("java.io.FilterInputStream"))) {
      return;
    }

    Optional<MethodTree> readByteIntInt = findMethod(classTree, READ_BYTES_INT_INT);
    if (!readByteIntInt.isPresent()) {
      String message = findMethod(classTree, READ_INT)
        .filter(readIntTree -> readIntTree.block().body().isEmpty())
        .map(readIntTree -> "Provide an empty override of \"read(byte[],int,int)\" for this class as well.")
        .orElse("Provide an override of \"read(byte[],int,int)\" for this class.");
      reportIssue(className, message);
    }

  }

  private static Optional<MethodTree> findMethod(ClassTree classTree, MethodMatcher methodMatcher) {
    return classTree.members()
      .stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(methodMatcher::matches)
      .findFirst();
  }
}
