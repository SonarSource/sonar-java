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
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S7475")
public class RemoveTypeFromUnusedPatternCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.RECORD_PATTERN);
  }

  @Override
  public void visitNode(Tree tree) {
    RecordPatternTree pattern = (RecordPatternTree) tree;
    for (PatternTree patternTree : pattern.patterns()) {
      if (patternTree instanceof TypePatternTree pat
        && pat.patternVariable().simpleName().isUnnamedVariable()
        && !typeIsMissing(pat.patternVariable())) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(pat.patternVariable())
          .withMessage("Remove unused type from unnamed pattern")
          .withQuickFix(() -> getQuickFix(pat.patternVariable().type()))
          .report();
      }
    }

  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava22Compatible();
  }

  private static JavaQuickFix getQuickFix(TypeTree tree) {
    return JavaQuickFix.newQuickFix("Remove unused type")
      .addTextEdit(JavaTextEdit.removeTree(tree))
      .build();
  }

  private static boolean typeIsMissing(VariableTree v) {
    return v.type() instanceof InferedTypeTree;
  }
}
