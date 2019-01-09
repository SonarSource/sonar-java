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
package org.sonar.java.se.checks.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.DebugCheck;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.CheckerDispatcher;
import org.sonar.java.se.Flow;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(
  key = "DEBUG-SE-MethodYieldsOnInvocations",
  name = "DEBUG(SE): Method yields on invocations",
  description = "Display method yields which will be used for each method invocation.",
  priority = Priority.INFO,
  tags = "debug")
public class DebugMethodYieldsOnInvocationsCheck extends SECheck implements DebugCheck {

  private Deque<List<MethodInvocationTree>> methodInvocations = new LinkedList<>();

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    methodInvocations.push(new ArrayList<>());
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      methodInvocations.peek().add((MethodInvocationTree) syntaxNode);
    }
    // No operation on state, just monitoring
    return context.getState();
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    reportAll(context);
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    reportAll(context);
  }

  private void reportAll(CheckerContext context) {
    CheckerDispatcher checkerDispatcher = (CheckerDispatcher) context;
    methodInvocations.pop().stream()
      .filter(mit -> mit.symbol().isMethodSymbol())
      .forEach(mit -> reportYields(mit, checkerDispatcher));
  }

  private void reportYields(MethodInvocationTree mit, CheckerDispatcher checkerDispatcher) {
    MethodBehavior mb = checkerDispatcher.peekMethodBehavior((Symbol.MethodSymbol) mit.symbol());
    if (mb != null && mb.isComplete()) {
      IdentifierTree methodName = getIdentifier(mit.methodSelect());
      String message = String.format("Method '%s' has %d method yields.", methodName.name(), mb.yields().size());
      Set<Flow> flow = flowFromYield(mb, methodName);
      reportIssue(methodName, message, flow);
    }
  }

  private static IdentifierTree getIdentifier(ExpressionTree methodSelect) {
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) methodSelect;
    }
    return ((MemberSelectExpressionTree) methodSelect).identifier();
  }

  private static Set<Flow> flowFromYield(MethodBehavior mb, IdentifierTree methodName) {
    Flow.Builder builder = Flow.builder();
    mb.yields().stream().map(yield -> new JavaFileScannerContext.Location(yield.toString(), methodName)).forEach(builder::add);
    return Collections.singleton(builder.build());
  }
}
