/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5790")
public class JunitNestedAnnotationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    IdentifierTree className = classTree.simpleName();
    if (className == null) {
      return;
    }
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isNestedClass(classSymbol) && hasJUnit5TestMethods(classTree)) {
      boolean hasNestedAnnotation = UnitTestUtils.hasNestedAnnotation(classTree);
      if (classSymbol.isStatic() && hasNestedAnnotation) {
        reportIssue(className, "Remove @Nested from this static nested test class or convert it into an inner class");
      } else if (!classSymbol.isStatic() && !hasNestedAnnotation) {
        reportIssue(className, "Add @Nested to this inner test class");
      }
    }
  }

  private static boolean hasJUnit5TestMethods(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .anyMatch(UnitTestUtils::hasJUnit5TestAnnotation);
  }

  private static boolean isNestedClass(Symbol.TypeSymbol classSymbol) {
    return !classSymbol.isAbstract() &&
      Optional.ofNullable(classSymbol.owner())
      .map(Symbol::isTypeSymbol)
      .orElse(false);
  }

}
