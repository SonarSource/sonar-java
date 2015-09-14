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
package org.sonar.java.symexecengine;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class SymbolicExecutionCheck {

  /**
   * called prior analysis of the given method.
   *
   * @param executionState execution state
   * @param tree method to be analyzed
   * @param arguments value of the arguments
   */
  protected void initialize(ExecutionState executionState, MethodTree tree, List<SymbolicValue> arguments) {
  }

  /**
  * @deprecated Required by CloseableVisitor. Should be properly documented if real needs arise.
  */
  @Deprecated
  protected void onAssignment(ExecutionState executionState, Tree tree, Symbol variable, ExpressionTree expression) {
  }

  /**
   * called when a constructor or method is invoked.
   *
   * @param executionState execution state
   * @param tree method invocation, constructor tree or new class tree
   */
  protected void onExecutableElementInvocation(ExecutionState executionState, Tree tree, List<ExpressionTree> arguments) {
  }

  /**
   * @deprecated called when a AutoCloseable resource of a try block is closed.
   *
   * @param executionState current execution state
   * @param tree declaration tree
   * @param resource value representing the autoclosed resource
   */
  // FIXME(merciesa): should probably be replaced by a call to close on the resource.
  @Deprecated
  protected void onTryResourceClosed(ExecutionState executionState, SymbolicValue resource) {
  }

  /**
   * called when a value is returned through a return statement.
   *
   * @param executionState execution state
   * @param tree tree
   * @param expression returned expression
   */
  protected void onValueReturned(ExecutionState executionState, ReturnStatementTree tree, ExpressionTree expression) {
  }

  /**
  * called when a value becomes unreachable.
   *
   * @param executionState execution state
   * @param state of the value when it became unreachable
   */
  protected void onValueUnreachable(ExecutionState executionState, State state) {
  }

}
