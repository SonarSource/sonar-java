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

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2976")
public class FileCreateTempFileCheck extends BaseTreeVisitor implements JavaFileScanner, JavaVersionAwareVisitor {

  private enum State {
    CREATE_TMP_FILE,
    DELETE,
    MKDIR
  }

  private static final String JAVA_IO_FILE = "java.io.File";
  private static final MethodMatchers FILE_CREATE_TEMP_FILE = MethodMatchers.create()
    .ofTypes(JAVA_IO_FILE).names("createTempFile").withAnyParameters().build();
  private static final MethodMatchers FILE_DELETE = MethodMatchers.create()
    .ofTypes(JAVA_IO_FILE).names("delete").addWithoutParametersMatcher().build();
  private static final MethodMatchers FILE_MKDIR = MethodMatchers.create()
    .ofTypes(JAVA_IO_FILE).names("mkdir").addWithoutParametersMatcher().build();

  private final Deque<Map<Symbol, State>> symbolStack = new LinkedList<>();
  private JavaFileScannerContext context;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    symbolStack.push(new HashMap<Symbol, State>());
    super.visitMethod(tree);
    symbolStack.pop();
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    if (isFileCreateTempFile(tree.expression())) {
      ExpressionTree variable = tree.variable();
      if (variable.is(Tree.Kind.IDENTIFIER) && !symbolStack.isEmpty()) {
        symbolStack.peek().put(((IdentifierTree) variable).symbol(), State.CREATE_TMP_FILE);
      }
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    ExpressionTree initializer = tree.initializer();
    if (initializer != null && isFileCreateTempFile(initializer)) {
      Symbol symbol = tree.symbol();
      if (!symbolStack.isEmpty()) {
        symbolStack.peek().put(symbol, State.CREATE_TMP_FILE);
      }
    }
  }

  private static boolean isFileCreateTempFile(ExpressionTree givenExpression) {
    ExpressionTree expressionTree = ExpressionUtils.skipParentheses(givenExpression);
    return expressionTree.is(Tree.Kind.METHOD_INVOCATION) && FILE_CREATE_TEMP_FILE.matches((MethodInvocationTree) expressionTree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    super.visitMethodInvocation(mit);
    if (FILE_DELETE.matches(mit)) {
      checkAndAdvanceState(mit, State.CREATE_TMP_FILE, State.DELETE);
    } else if (FILE_MKDIR.matches(mit) && State.MKDIR.equals(checkAndAdvanceState(mit, State.DELETE, State.MKDIR))) {
      context.reportIssue(
        this,
        ExpressionUtils.methodName(mit),
        "Use \"Files.createTempDirectory\" or a library function to create this directory instead." +
          context.getJavaVersion().java7CompatibilityMessage());
    }
  }

  @Nullable
  private State checkAndAdvanceState(MethodInvocationTree mit, State requiredState, State nextState) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expressionTree = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) expressionTree).symbol();
        Map<Symbol, State> symbolStateMap = symbolStack.peek();
        if (symbolStateMap != null && symbolStateMap.containsKey(symbol) && requiredState.equals(symbolStateMap.get(symbol))) {
          symbolStateMap.put(symbol, nextState);
          return nextState;
        }
      }
    }
    return null;
  }
}
