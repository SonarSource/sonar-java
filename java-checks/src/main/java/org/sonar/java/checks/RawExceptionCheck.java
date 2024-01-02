/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00112", repositoryKey = "squid")
@Rule(key = "S112")
public class RawExceptionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final List<String> RAW_EXCEPTIONS = Arrays.asList(
    "java.lang.Throwable",
    "java.lang.Error",
    "java.lang.Exception",
    "java.lang.RuntimeException");

  private FluentReporting context;
  private final Set<Type> exceptionsThrownByMethodInvocations = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = (FluentReporting) context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree method) {
    super.visitMethod(method);
    if ((method.is(Tree.Kind.CONSTRUCTOR) || isNotOverridden(method)) && isNotMainMethod(method) && hasNoUnknownMethod(method)) {
      for (TypeTree throwClause : method.throwsClauses()) {
        Type exceptionType = throwClause.symbolType();
        if (isRawException(exceptionType) && !exceptionsThrownByMethodInvocations.contains(exceptionType)) {
          reportIssue(throwClause);
        }
      }
    }
    exceptionsThrownByMethodInvocations.clear();
  }

  private static boolean hasNoUnknownMethod(MethodTree method) {
    MethodTreeUtils.MethodInvocationCollector unknownMethodVisitor = new MethodTreeUtils.MethodInvocationCollector(Symbol::isUnknown);
    method.accept(unknownMethodVisitor);
    return unknownMethodVisitor.getInvocationTree().isEmpty();
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    if (tree.expression().is(Tree.Kind.NEW_CLASS)) {
      TypeTree exception = ((NewClassTree) tree.expression()).identifier();
      if (isRawException(exception.symbolType())) {
        reportIssue(exception);
      }
    }
    super.visitThrowStatement(tree);
  }

  private void reportIssue(Tree tree) {
    context.newIssue()
      .forRule(this)
      .onTree(tree)
      .withMessage("Define and throw a dedicated exception instead of using a generic one.")
      .report();
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    collectThrownTypes(mit.methodSymbol());
    super.visitMethodInvocation(mit);
  }

  @Override
  public void visitNewClass(NewClassTree nct) {
    collectThrownTypes(nct.methodSymbol());
    super.visitNewClass(nct);
  }

  private void collectThrownTypes(Symbol.MethodSymbol symbol) {
    if (!symbol.isUnknown()) {
      exceptionsThrownByMethodInvocations.addAll(symbol.thrownTypes());
    }
  }

  private static boolean isRawException(Type type) {
    return RAW_EXCEPTIONS.stream().anyMatch(type::is);
  }

  private static boolean isNotOverridden(MethodTree tree) {
    return Boolean.FALSE.equals(tree.isOverriding());
  }

  private static boolean isNotMainMethod(MethodTree tree) {
    return !MethodTreeUtils.isMainMethod(tree);
  }

}
