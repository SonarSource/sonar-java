/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Pattern;

@Rule(
  key = "S00120",
  priority = Priority.MAJOR,
  tags={"convention"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class BadPackageName_S00120_Check extends BaseTreeVisitor implements JavaFileScanner {

  private static final String RULE_KEY = "S00120";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private static final String DEFAULT_FORMAT = "^[a-z]+(\\.[a-z][a-z0-9]*)*$";

  @RuleProperty(
    key = "format",
    defaultValue = DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    if (tree.packageName() != null) {
      String name = concatenate(tree.packageName());
      if (!pattern.matcher(name).matches()) {
        context.addIssue(tree, ruleKey, "Rename this package name to match the regular expression '" + format + "'.");
      }
    }
  }

  private String concatenate(ExpressionTree tree) {
    Deque<String> pieces = new LinkedList<String>();

    ExpressionTree expr = tree;
    while (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      pieces.push(mse.identifier().name());
      pieces.push(".");
      expr = mse.expression();
    }
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree idt = (IdentifierTree) expr;
      pieces.push(idt.name());
    }

    StringBuilder sb = new StringBuilder();
    for (String piece: pieces) {
      sb.append(piece);
    }
    return sb.toString();
  }

}
