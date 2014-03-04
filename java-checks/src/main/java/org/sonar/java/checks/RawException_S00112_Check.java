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

import com.google.common.collect.ImmutableSet;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Set;

@Rule(
    key = RawException_S00112_Check.RULE_KEY,
    priority = Priority.MAJOR,
    tags={"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class RawException_S00112_Check extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S00112";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private static final Set<String> RAW_EXCEPTIONS = ImmutableSet.of("Throwable", "Error", "Exception", "RuntimeException");

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (!isOverride(tree)) {
      for (ExpressionTree throwClause : tree.throwsClauses()) {
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
      context.addIssue(tree, ruleKey, "Define and throw a dedicated exception instead of using a generic one.");
    }
  }

  private boolean isRawException(Tree tree) {
    return tree.is(Tree.Kind.IDENTIFIER) && RAW_EXCEPTIONS.contains(((IdentifierTree) tree).name());
  }

  private boolean isOverride(MethodTree tree) {
    for (AnnotationTree annotationTree : tree.modifiers().annotations()) {
      if (annotationTree.annotationType().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) annotationTree.annotationType();
        if (Override.class.getSimpleName().equals(identifier.name())) {
          return true;
        }
      }
    }
    return false;
  }

}
