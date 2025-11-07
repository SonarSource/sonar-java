/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.naming;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.regex.Pattern;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00117", repositoryKey = "squid")
@Rule(key = "S117")
public class BadLocalVariableNameCheck  extends BaseTreeVisitor implements JavaFileScanner {

  private static final String DEFAULT_FORMAT = "^[a-z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the names against.",
    defaultValue = "" + DEFAULT_FORMAT)
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
  public void visitClass(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        // skip check of field
        scan(((VariableTree) member).initializer());
      } else {
        scan(member);
      }
    }
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.statement());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    scan(tree.statement());
  }

  @Override
  public void visitCatch(CatchTree tree) {
    VariableTree parameter = tree.parameter();
    if (parameter.simpleName().name().length() > 1) {
      scan(parameter);
    }
    scan(tree.block());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (!simpleName.isUnnamedVariable() && !pattern.matcher(simpleName.name()).matches() && !isLocalConstant(tree)) {
      context.reportIssue(this, simpleName, "Rename this local variable to match the regular expression '" + format + "'.");
    }
    super.visitVariable(tree);
  }

  private boolean isLocalConstant(VariableTree tree) {
    return context.getSemanticModel() != null && isConstantType(tree.symbol().type()) && tree.symbol().isFinal();
  }

  private static boolean isConstantType(Type symbolType) {
    return symbolType.isPrimitive() || symbolType.is("java.lang.String") || symbolType.isPrimitiveWrapper();
  }

}
