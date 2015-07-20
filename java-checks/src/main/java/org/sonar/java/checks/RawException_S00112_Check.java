/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Set;

@Rule(
  key = "S00112",
  name = "Generic exceptions should never be thrown",
  tags = {"cwe", "error-handling", "security"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.EXCEPTION_HANDLING)
@SqaleConstantRemediation("20min")
public class RawException_S00112_Check extends BaseTreeVisitor implements JavaFileScanner {

  private static final Set<String> RAW_EXCEPTIONS = ImmutableSet.of("Throwable", "Error", "Exception", "RuntimeException");

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if ((tree.is(Tree.Kind.CONSTRUCTOR) || isNotOverriden(tree)) && !((MethodTreeImpl) tree).isMainMethod()) {
      for (TypeTree throwClause : tree.throwsClauses()) {
        checkExceptionAndRaiseIssue(throwClause);
      }
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    if (tree.expression().is(Tree.Kind.NEW_CLASS)) {
      checkExceptionAndRaiseIssue(((NewClassTree) tree.expression()).identifier());
    }
    super.visitThrowStatement(tree);
  }

  private void checkExceptionAndRaiseIssue(Tree tree) {
    if (isRawException(tree)) {
      context.addIssue(tree, this, "Define and throw a dedicated exception instead of using a generic one.");
    }
  }

  private static boolean isRawException(Tree tree) {
    return tree.is(Tree.Kind.IDENTIFIER) && RAW_EXCEPTIONS.contains(((IdentifierTree) tree).name());
  }

  private static boolean isNotOverriden(MethodTree tree) {
    return BooleanUtils.isFalse(((MethodTreeImpl) tree).isOverriding());
  }

}
