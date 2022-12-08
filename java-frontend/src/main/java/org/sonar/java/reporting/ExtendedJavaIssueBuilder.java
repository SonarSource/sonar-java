/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.reporting;

import java.util.List;
import java.util.function.Supplier;
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Extends the {@link FluentReporting.JavaIssueBuilder} with quick-fix capabilities.
 */
@Beta
public interface ExtendedJavaIssueBuilder extends FluentReporting.JavaIssueBuilder {

  @Override
  ExtendedJavaIssueBuilder forRule(JavaCheck rule);

  @Override
  ExtendedJavaIssueBuilder onTree(Tree tree);

  @Override
  ExtendedJavaIssueBuilder onRange(Tree from, Tree to);

  @Override
  ExtendedJavaIssueBuilder withMessage(String message);

  /**
   * Alias for java.lang.String.format(String, Object...)
   */
  @Override
  ExtendedJavaIssueBuilder withMessage(String message, Object... args);

  @Override
  ExtendedJavaIssueBuilder withSecondaries(JavaFileScannerContext.Location... secondaries);

  @Override
  ExtendedJavaIssueBuilder withSecondaries(List<JavaFileScannerContext.Location> secondaries);

  @Override
  ExtendedJavaIssueBuilder withFlows(List<List<JavaFileScannerContext.Location>> flows);

  @Override
  ExtendedJavaIssueBuilder withCost(int cost);

  ExtendedJavaIssueBuilder withQuickFix(Supplier<JavaQuickFix> quickFixes);

  ExtendedJavaIssueBuilder withQuickFixes(Supplier<List<JavaQuickFix>> quickFixes);

}
