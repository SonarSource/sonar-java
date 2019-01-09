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
package org.sonar.java.se;

import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Set;

public interface CheckerContext {

  Object createSink();

  void reportIssue(Tree tree, SECheck check, String message);

  void reportIssue(Tree tree, SECheck check, String message, Set<Flow> flows);

  void addTransition(ProgramState state);

  void addExceptionalYield(SymbolicValue target, ProgramState exceptionalState, String exceptionFullyQualifiedName, SECheck check);

  ProgramState getState();

  ExplodedGraph.Node getNode();

  ConstraintManager getConstraintManager();

  AlwaysTrueOrFalseExpressionCollector alwaysTrueOrFalseExpressions();
}
