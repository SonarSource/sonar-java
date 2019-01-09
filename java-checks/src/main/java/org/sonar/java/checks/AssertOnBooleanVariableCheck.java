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
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S3346")
public class AssertOnBooleanVariableCheck extends IssuableSubscriptionVisitor {

  private static final Pattern SIDE_EFFECT_METHOD_NAMES = Pattern.compile("^(remove|delete|retain|put|set|add|pop|update).*$", Pattern.CASE_INSENSITIVE);

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ASSERT_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ((AssertStatementTree) tree).condition().accept(new MethodInvocationVisitor());
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      IdentifierTree methodNameTree = ExpressionUtils.methodName(tree);
      if (SIDE_EFFECT_METHOD_NAMES.matcher(methodNameTree.name()).find()) {
        reportIssue(methodNameTree, "Move this \"assert\" side effect to another statement.");
      } else {
        // only report once
        super.visitMethodInvocation(tree);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip anonymous classes
    }
  }

}
