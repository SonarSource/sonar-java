/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se;

import java.util.Set;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

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

  JavaFileScannerContext getScannerContext();
}
