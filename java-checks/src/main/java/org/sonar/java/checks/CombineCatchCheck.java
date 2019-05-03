/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2147")
public class CombineCatchCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    List<CatchTree> catches = new ArrayList<>();
    for (CatchTree catchTree : ((TryStatementTree) tree).catches()) {
      for (CatchTree catchTreeToBeCompared : catches) {
        if (SyntacticEquivalence.areEquivalent(catchTree.block(), catchTreeToBeCompared.block())) {
          reportIssue(catchTree, catchTreeToBeCompared);
          break;
        }
      }
      catches.add(catchTree);
    }
  }

  private void reportIssue(CatchTree catchTree, CatchTree catchTreeToBeCompared) {
    String message = "Combine this catch with the one at line " + catchTreeToBeCompared.catchKeyword().line()
      + ", which has the same body." + context.getJavaVersion().java7CompatibilityMessage();
    List<JavaFileScannerContext.Location> flow = Lists.newArrayList(new JavaFileScannerContext.Location("Combine with this catch", catchTreeToBeCompared));
    reportIssue(catchTree.parameter(), message, flow, null);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isNotSet() || version.asInt() >= 7;
  }
}
