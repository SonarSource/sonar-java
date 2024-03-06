/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.VariableTree;


@Rule(key = "S6878")
public class RecordPatternInsteadOfFieldAccessCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final List<String> ALLOWED_METHODS = List.of("toString", "hashCode", "equals");

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.PATTERN_INSTANCE_OF, Tree.Kind.CASE_LABEL);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof PatternInstanceOfTree instanceOf) {
      var pattern = instanceOf.pattern();
      if (pattern instanceof TypePatternTree typePattern && isRecordPattern(typePattern)) {
        checkTypePatternVariableUsage(typePattern.patternVariable());
      }
    } else {
      checkCaseLabel((CaseLabelTree) tree);
    }
  }

  private void checkCaseLabel(CaseLabelTree caseLabel) {
    var typePattern = getTypePatternFromCaseGroup(caseLabel);
    typePattern.ifPresent(typePatternTree -> checkTypePatternVariableUsage(typePatternTree.patternVariable()));
  }

  private static Optional<TypePatternTree> getTypePatternFromCaseGroup(CaseLabelTree caseLabel) {
    if (caseLabel.expressions().size() == 1 && caseLabel.expressions().get(0) instanceof TypePatternTree typePattern) {
      return Optional.of(typePattern);
    }
    return Optional.empty();
  }

  private void checkTypePatternVariableUsage(VariableTree patternVariable) {
    var secondaryLocationsTrees = new ArrayList<Tree>();
    for (Tree usage : patternVariable.symbol().usages()) {
      if (usage.parent() instanceof MemberSelectExpressionTree mse && isNotRecordGetter(mse)) {
        secondaryLocationsTrees.add(mse);
      } else {
        return;
      }
    }
    reportIssue(patternVariable, "Use the record pattern instead of this pattern match variable.",
      getSecondaryLocations(secondaryLocationsTrees), null);
  }

  private static boolean isNotRecordGetter(MemberSelectExpressionTree mse) {
    return !ALLOWED_METHODS.contains(mse.identifier().name());
  }

  private static List<JavaFileScannerContext.Location> getSecondaryLocations(List<Tree> secondaryLocationsTrees) {
    return secondaryLocationsTrees.stream()
      .map(tree ->
        new JavaFileScannerContext.Location("Replace this getter with the respective record pattern component", tree))
      .toList();
  }

  private static boolean isRecordPattern(TypePatternTree typePattern) {
    return typePattern.patternVariable().type().symbolType().isSubtypeOf("java.lang.Record");
  }

}
