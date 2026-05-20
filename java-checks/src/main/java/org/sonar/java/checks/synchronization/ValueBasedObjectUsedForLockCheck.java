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
package org.sonar.java.checks.synchronization;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.TypeUtils.isValueBasedType;

@Rule(key = "S3436")
public class ValueBasedObjectUsedForLockCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expression = ((SynchronizedStatementTree) tree).expression();
    Type expressionType = expression.symbolType();
    if (isValueBasedType(expressionType)) {
      reportIssue(expression, String.format("Synchronize on a non-value-based object; synchronizing on a \"%s\" could lead to contention.%s",
        expressionType.name(),
        context.getJavaVersion().java8CompatibilityMessage()));
    }
  }

}
