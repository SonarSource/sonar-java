/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S1075")
public class HardcodedURICheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.net.URI")
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING).build(),
    MethodMatchers.create()
      .ofTypes("java.io.File")
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING)
      .addParametersMatcher(ANY, JAVA_LANG_STRING)
      .build());

  private static final String SCHEME = "[a-zA-Z][a-zA-Z\\+\\.\\-]+";
  private static final String FOLDER_NAME = "[^/?%*:\\\\|\"<>]+";
  private static final String URI_REGEX = String.format("^%s://.+", SCHEME);
  private static final String LOCAL_URI = String.format("^(~/|/|//[\\w-]+/|%s:/)(%s/)*%s/?",
    SCHEME, FOLDER_NAME, FOLDER_NAME);
  private static final String BACKSLASH_LOCAL_URI = String.format("^(~\\\\\\\\|\\\\\\\\\\\\\\\\[\\w-]+\\\\\\\\|%s:\\\\\\\\)(%s\\\\\\\\)*%s(\\\\\\\\)?",
    SCHEME, FOLDER_NAME, FOLDER_NAME);
  private static final String DISK_URI = "^[A-Za-z]:(/|\\\\)";

  private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX + "|" + LOCAL_URI + "|" + DISK_URI + "|" + BACKSLASH_LOCAL_URI);
  private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("filename|path", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATH_DELIMETERS_PATTERN = Pattern.compile("\"/\"|\"//\"|\"\\\\\\\\\"|\"\\\\\\\\\\\\\\\\\"");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.NEW_CLASS, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      checkNewClassTree((NewClassTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      checkVariable((VariableTree) tree);
    } else {
      checkAssignment((AssignmentExpressionTree) tree);
    }
  }

  private void checkNewClassTree(NewClassTree nct) {
    if (MATCHERS.matches(nct)) {
      nct.arguments().forEach(this::checkExpression);
    }
  }

  private void checkVariable(VariableTree tree) {
    if (isFileNameVariable(tree.simpleName())) {
      checkExpression(tree.initializer());
    }
  }

  private void checkAssignment(AssignmentExpressionTree tree) {
    if (isFileNameVariable(getVariableIdentifier(tree)) && !isPartOfAnnotation(tree)) {
      checkExpression(tree.expression());
    }
  }

  private static boolean isPartOfAnnotation(AssignmentExpressionTree tree) {
    Tree parent = tree.parent();
    while (parent != null) {
      if (parent.is(Tree.Kind.ANNOTATION)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static boolean isFileNameVariable(@Nullable IdentifierTree variable) {
    return variable != null && VARIABLE_NAME_PATTERN.matcher(variable.name()).find();
  }

  private void checkExpression(@Nullable ExpressionTree expr) {
    if (expr != null) {
      if (isHardcodedURI(expr)) {
        reportHardcodedURI(expr);
      } else {
        reportStringConcatenationWithPathDelimiter(expr);
      }
    }
  }

  private static boolean isHardcodedURI(ExpressionTree expr) {
    ExpressionTree newExpr = ExpressionUtils.skipParentheses(expr);
    if (!newExpr.is(Tree.Kind.STRING_LITERAL)) {
      return false;
    }
    String stringLiteral = LiteralUtils.trimQuotes(((LiteralTree) newExpr).value());
    return URI_PATTERN.matcher(stringLiteral).find();
  }

  private void reportHardcodedURI(ExpressionTree hardcodedURI) {
    reportIssue(hardcodedURI, "Refactor your code to get this URI from a customizable parameter.");
  }

  private void reportStringConcatenationWithPathDelimiter(ExpressionTree expr) {
    expr.accept(new StringConcatenationVisitor());
  }

  private class StringConcatenationVisitor extends BaseTreeVisitor {
    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.PLUS)) {
        checkPathDelimiter(tree.leftOperand());
        checkPathDelimiter(tree.rightOperand());
      }
      super.visitBinaryExpression(tree);
    }

    private void checkPathDelimiter(ExpressionTree expr) {
      ExpressionTree newExpr = ExpressionUtils.skipParentheses(expr);
      if (newExpr.is(Tree.Kind.STRING_LITERAL) && PATH_DELIMETERS_PATTERN.matcher(((LiteralTree) newExpr).value()).find()) {
        reportIssue(newExpr, "Remove this hard-coded path-delimiter.");
      }
    }
  }

  @CheckForNull
  private static IdentifierTree getVariableIdentifier(AssignmentExpressionTree tree) {
    ExpressionTree variable = ExpressionUtils.skipParentheses(tree.variable());
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) variable;
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier();
    }
    // ignore assignments in arrays
    return null;
  }

}
