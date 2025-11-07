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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

public class EndOfAnalysisVisitor extends SubscriptionVisitor implements EndOfAnalysis {

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    // do nothing
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.COMPILATION_UNIT);
  }
}
