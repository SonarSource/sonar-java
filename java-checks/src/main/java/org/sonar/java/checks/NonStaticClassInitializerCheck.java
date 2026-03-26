/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1171")
public class NonStaticClassInitializerCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INITIALIZER);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!isMemberOfAnonymousClass(tree)) {
      reportIssue(((BlockTree) tree).openBraceToken(), "Move the contents of this initializer to a standard constructor or to field initializers.");
    }
  }

  private static boolean isMemberOfAnonymousClass(Tree tree) {
    Tree parent = tree.parent();
    return parent.is(Tree.Kind.CLASS) && parent.parent() != null && parent.parent().is(Tree.Kind.NEW_CLASS);
  }
}
