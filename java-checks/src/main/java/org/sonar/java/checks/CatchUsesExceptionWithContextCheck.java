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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

@Rule(key = "S1166")
public class CatchUsesExceptionWithContextCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String EXCLUDED_EXCEPTION_TYPE = "java.lang.InterruptedException, " +
      "java.lang.NumberFormatException, " +
      "java.lang.NoSuchMethodException, " +
      "java.text.ParseException, " +
      "java.net.MalformedURLException, " +
      "java.time.format.DateTimeParseException";


  @RuleProperty(
      key = "exceptions",
      description = "List of exceptions which should not be checked",
      defaultValue = "" + EXCLUDED_EXCEPTION_TYPE)
  public String exceptionsCommaSeparated = EXCLUDED_EXCEPTION_TYPE;

  private JavaFileScannerContext context;
  private Deque<Collection<IdentifierTree>> validUsagesStack;
  private Iterable<String> exceptions;
  private List<String> exceptionIdentifiers;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    validUsagesStack = new ArrayDeque<>();
    exceptions = Splitter.on(",").trimResults().split(exceptionsCommaSeparated);
    exceptionIdentifiers = Lists.newArrayList();
    for (String exception : exceptions) {
      exceptionIdentifiers.add(exception.substring(exception.lastIndexOf('.') + 1));
    }
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    if (!isExcludedType(tree.parameter().type())) {
      Symbol exception = tree.parameter().symbol();
      validUsagesStack.addFirst(Lists.newArrayList(exception.usages()));
      super.visitCatch(tree);
      Collection<IdentifierTree> usages = validUsagesStack.pop();
      if (usages.isEmpty()) {
        context.reportIssue(this, tree.parameter(), "Either log or rethrow this exception.");
      }
    }
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    IdentifierTree identifier = null;
    ExpressionTree expression = tree.expression();
    if (expression.is(Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) expression;
    } else if (expression.is(Kind.PARENTHESIZED_EXPRESSION) && ((ParenthesizedTree) expression).expression().is(Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) ((ParenthesizedTree) expression).expression();
    }
    if (!validUsagesStack.isEmpty() && identifier != null) {
      Iterator<Collection<IdentifierTree>> iterator = validUsagesStack.iterator();
      while (iterator.hasNext()) {
        iterator.next().remove(identifier);
      }
    }
    super.visitMemberSelectExpression(tree);

  }

  private boolean isExcludedType(Tree tree) {
    return isUnqualifiedExcludedType(tree) ||
        isQualifiedExcludedType(tree);
  }

  private boolean isUnqualifiedExcludedType(Tree tree) {
    return tree.is(Kind.IDENTIFIER) &&
        exceptionIdentifiers.contains(((IdentifierTree) tree).name());
  }

  private boolean isQualifiedExcludedType(Tree tree) {
    if (!tree.is(Kind.MEMBER_SELECT)) {
      return false;
    }
    return Iterables.contains(exceptions, ExpressionsHelper.concatenate((MemberSelectExpressionTree) tree));
  }

}
