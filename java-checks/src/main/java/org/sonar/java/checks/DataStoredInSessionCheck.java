/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ReassignmentFinder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;

@Rule(key = "S3318")
public class DataStoredInSessionCheck extends AbstractMethodDetection {

  private Set<IdentifierTree> identifiersUsedToSetAttribute;

  private static final MethodMatcherCollection REQUEST_OR_COOKIE_DATA_RETRIEVAL = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("javax.servlet.http.Cookie").name(NameCriteria.startsWith("get")).withAnyParameters(),
    MethodMatcher.create().callSite(TypeCriteria.is("javax.servlet.http.HttpServletRequest")).name(NameCriteria.startsWith("get")).withAnyParameters());

  private static final MethodMatcherCollection NO_EFFECT_OPERATION = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.net.URLDecoder").name("decode").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.apache.commons.lang.StringEscapeUtils").name("escapeHtml").withAnyParameters());

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition("javax.servlet.http.HttpSession").name("setAttribute").addParameter(TypeCriteria.anyType()).addParameter(TypeCriteria.anyType()),
      MethodMatcher.create().typeDefinition("javax.servlet.http.HttpSession").name("putValue").addParameter(TypeCriteria.anyType()).addParameter(TypeCriteria.anyType()));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    for (ExpressionTree argument : mit.arguments()) {
      checkArgument(argument, mit, mit);
    }
  }

  private void checkArgument(ExpressionTree argument, ExpressionTree startPoint, MethodInvocationTree reportTree) {
    ExpressionTree expressionToEvaluate = argument;
    if (expressionToEvaluate.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expressionToEvaluate;
      identifiersUsedToSetAttribute.add(identifier);
      Symbol variable = identifier.symbol();
      ExpressionTree lastAssignmentOrDeclaration = ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(startPoint, variable);
      if (lastAssignmentOrDeclaration != null && !usedBetween(variable, lastAssignmentOrDeclaration, startPoint)) {
        expressionToEvaluate = lastAssignmentOrDeclaration;
      }
    }

    if (isRequestOrCookieDataRetrieval(expressionToEvaluate)) {
      reportIssue(reportTree.methodSelect(), "Make sure the user is authenticated before this data is stored in the session.");
    } else if (expressionToEvaluate.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expressionToEvaluate;
      if (NO_EFFECT_OPERATION.anyMatch(mit)) {
        checkArgument(mit.arguments().get(0), mit, reportTree);
      }
    }
  }

  private boolean usedBetween(Symbol variable, Tree start, Tree end) {
    SyntaxToken startToken = start.lastToken();
    SyntaxToken endToken = end.firstToken();

    for (IdentifierTree identifier : variable.usages()) {
      SyntaxToken identifierToken = identifier.identifierToken();
      if (isAfterFirstToken(identifierToken, startToken) && isBeforeLastToken(identifierToken, endToken) && !identifiersUsedToSetAttribute.contains(identifier)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAfterFirstToken(SyntaxToken token, SyntaxToken firstToken) {
    int firstTokenLine = firstToken.line();
    int tokenLine = token.line();
    return tokenLine > firstTokenLine || (tokenLine == firstTokenLine && firstToken.column() < token.column());
  }

  private static boolean isBeforeLastToken(SyntaxToken token, SyntaxToken lastToken) {
    int lastTokenLine = lastToken.line();
    int tokenLine = token.line();
    return tokenLine < lastTokenLine || (tokenLine == lastTokenLine && lastToken.column() > token.column());
  }

  private static boolean isRequestOrCookieDataRetrieval(ExpressionTree expr) {
    return expr.is(Tree.Kind.METHOD_INVOCATION) && REQUEST_OR_COOKIE_DATA_RETRIEVAL.anyMatch((MethodInvocationTree) expr);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    identifiersUsedToSetAttribute = Sets.newHashSet();
    super.scanFile(context);
    identifiersUsedToSetAttribute.clear();
  }
}
