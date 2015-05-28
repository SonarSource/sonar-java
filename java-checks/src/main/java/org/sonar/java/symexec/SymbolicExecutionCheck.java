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
package org.sonar.java.symexec;

import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public class SymbolicExecutionCheck {

  /**
   * called when a condition is evaluated.
   *
   * @param executionState execution state
   * @param tree parent tree (if, do...while, for, switch)
   * @param result result of the computation
   */
  protected void onCondition(ExecutionState executionState, Tree tree, SymbolicValue result) {
  }

  /**
   * called when a constructor or method is invoked.
   *
   * @param executionState execution state
   * @param tree method invocation, constructor tree or new class tree
   * @param arguments symbolic values of the arguments
   */
  protected void onExecutableElementInvocation(ExecutionState executionState, Tree tree, List<SymbolicValue> arguments) {
  }

}
