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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.pattern.RecordPatternTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S7473")
public class UnusedVarInPatternMatchingCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.RECORD_PATTERN);
  }

  @Override
  public void visitNode(Tree tree) {
    RecordPatternTreeImpl rpt = (RecordPatternTreeImpl) tree;
    if (checkAllTypePatternVariablesAreUnused(rpt)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onRange(rpt.openParenToken(), rpt.closeParenToken())
        .withMessage("Remove this unused record pattern matching.")
        .withQuickFix(() -> buildQuickFix(rpt))
        .report();
    }
  }

  /**
   * @param recordPatternTree the record pattern tree whose deconstructed components type patterns we want to check the usages for
   * @return true if all the records type patterns are unused, false otherwise
   */
  private static boolean checkAllTypePatternVariablesAreUnused(RecordPatternTreeImpl recordPatternTree) {
    var usedTypePatterns = recordPatternTree.patterns().stream()
      .map(TypePatternTreeImpl.class::cast)
      .map(tp -> tp.patternVariable().symbol())
      .filter(sym -> !sym.usages().isEmpty())
      .toList();
    return usedTypePatterns.isEmpty();
  }

  private static JavaQuickFix buildQuickFix(RecordPatternTreeImpl rpt) {
    return JavaQuickFix.newQuickFix("Remove the record deconstruction.")
      .addTextEdit(JavaTextEdit.replaceBetweenTree(rpt.openParenToken(), rpt.closeParenToken(), ""))
      .build();
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava22Compatible();
  }
}
