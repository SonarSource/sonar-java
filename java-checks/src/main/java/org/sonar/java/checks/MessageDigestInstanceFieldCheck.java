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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9428")
public class MessageDigestInstanceFieldCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_DIGEST = "java.security.MessageDigest";
  private static final String MESSAGE = "Make this MessageDigest a local variable.";

  private boolean hasSemantic;

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    hasSemantic = context.getSemanticModel() != null;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic) {
      return;
    }
    VariableTree variable = (VariableTree) tree;
    Symbol symbol = variable.symbol();
    Type type = symbol.type();
    if (symbol.owner().isTypeSymbol() && !type.isUnknown() && type.isSubtypeOf(MESSAGE_DIGEST)) {
      reportIssue(variable.simpleName(), MESSAGE);
    }
  }
}
