/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.reporting;

import java.util.List;
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

@Beta
public interface FluentReporting {

  JavaIssueBuilder newIssue();

  interface JavaIssueBuilder {
    JavaIssueBuilder forRule(JavaCheck rule);

    JavaIssueBuilder onTree(Tree tree);

    JavaIssueBuilder onRange(Tree from, Tree to);

    JavaIssueBuilder withMessage(String message);

    /**
     * Alias for java.lang.String.format(String, Object...)
     */
    JavaIssueBuilder withMessage(String message, Object... args);

    JavaIssueBuilder withSecondaries(JavaFileScannerContext.Location... secondaries);

    JavaIssueBuilder withSecondaries(List<JavaFileScannerContext.Location> secondaries);

    JavaIssueBuilder withFlows(List<List<JavaFileScannerContext.Location>> flows);

    JavaIssueBuilder withCost(int cost);

    void report();
  }
}
