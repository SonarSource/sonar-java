/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.ast.visitors;

import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class NumberOfAccessedVariablesVisitor extends BaseTreeVisitor {
  private int numberOfAccessedVariables = 0;

  @Override
  public void visitVariable(VariableTree tree) {
    numberOfAccessedVariables++;
  }

  public int getNumberOfAccessedVariables(MethodTree tree) {
    numberOfAccessedVariables = 0;
    scan(tree);
    return numberOfAccessedVariables;
  }

}
