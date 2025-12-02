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

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00104", repositoryKey = "squid")
@Rule(key = "S104")
public class TooManyLinesOfCodeInFileCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAXIMUM = 750;

  @RuleProperty(
      key = "Max",
      description = "Maximum authorized lines in a file.",
      defaultValue = "" + DEFAULT_MAXIMUM)
  public int maximum = DEFAULT_MAXIMUM;


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
    int lines = metricsComputer.getLinesOfCode(tree);
    if (lines > maximum) {
      addIssueOnFile(MessageFormat.format("This file has {0} lines, which is greater than {1} authorized. Split it into smaller files.", lines, maximum));
    }
  }
}
