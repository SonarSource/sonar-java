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

import com.google.common.collect.ImmutableSet;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Set;

@Rule(key = "S1317")
public class StringBufferAndBuilderWithCharCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private static final Set<String> TARGETED_CLASS = ImmutableSet.of("StringBuilder", "StringBuffer");

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (TARGETED_CLASS.contains(getclassName(tree)) && tree.arguments().size() == 1) {
      ExpressionTree argument = tree.arguments().get(0);

      if (argument.is(Tree.Kind.CHAR_LITERAL)) {
        String character = ((LiteralTree) argument).value();
        context.reportIssue(this, argument, "Replace the constructor character parameter " + character + " with string parameter " + character.replace("'", "\"") + ".");
      }
    }
  }

  public static String getclassName(NewClassTree newClasstree) {
    if (newClasstree.identifier().is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) newClasstree.identifier()).identifier().name();
    } else if (newClasstree.identifier().is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) newClasstree.identifier()).name();
    }
    return null;
  }

}
