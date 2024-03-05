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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.VariableTree;


@Rule(key = "S6878")
public class RecordPatternInsteadOfFieldAccessCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.PATTERN_INSTANCE_OF);
  }

  @Override
  public void visitNode(Tree tree) {
    PatternInstanceOfTree instanceOf = (PatternInstanceOfTree) tree;
    var pattern = instanceOf.pattern();
    if (pattern instanceof TypePatternTree typePattern &&
      typePattern.patternVariable().type().symbolType().isSubtypeOf("java.lang.Record")) {
      checkTypePatternVariableUsage(typePattern.patternVariable());
    }
  }

  private void checkTypePatternVariableUsage(VariableTree patternVariable) {
    for(Tree usage : patternVariable.symbol().usages()) {
      reportIssue(usage, "Use the record pattern instead of field access.");
    }
  }

}
