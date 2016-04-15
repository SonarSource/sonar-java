/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

@Rule(key = "S1696")
public class CatchNPECheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCatch(CatchTree tree) {
    super.visitCatch(tree);

    Tree typeTree = tree.parameter().type();

    if (typeTree.is(Kind.UNION_TYPE)) {
      UnionTypeTree unionTypeTree = (UnionTypeTree) typeTree;
      for (Tree typeAlternativeTree : unionTypeTree.typeAlternatives()) {
        checkType(typeAlternativeTree);
      }
    } else {
      checkType(typeTree);
    }
  }

  private void checkType(Tree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) tree;
      if (isNPE(identifier.name())) {
        addIssue(identifier);
      }
    } else if (tree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectTree = (MemberSelectExpressionTree) tree;

      if (isNPE(memberSelectTree.identifier().name())) {
        ExpressionTree tree2 = memberSelectTree.expression();

        if (tree2.is(Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree memberSelectTree2 = (MemberSelectExpressionTree) tree2;

          if ("lang".equals(memberSelectTree2.identifier().name()) &&
              memberSelectTree2.expression().is(Kind.IDENTIFIER) &&
              "java".equals(((IdentifierTree) memberSelectTree2.expression()).name())) {
            addIssue(memberSelectTree.identifier());
          }
        }
      }
    }
  }

  private static boolean isNPE(String name) {
    return "NullPointerException".equals(name);
  }

  private void addIssue(IdentifierTree tree) {
    context.reportIssue(this, tree, "Avoid catching NullPointerException.");
  }

}
