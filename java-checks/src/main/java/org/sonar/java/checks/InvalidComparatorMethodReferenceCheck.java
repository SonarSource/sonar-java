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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9400")
public class InvalidComparatorMethodReferenceCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Replace this method reference; \"%s\" does not return a comparison result, violating the \"Comparator\" contract.";

  private static final MethodMatchers INVALID_METHODS = MethodMatchers.create()
    .ofTypes(
      "java.lang.Math",
      "java.lang.StrictMath",
      "java.lang.Integer"
    )
    .names("min", "max", "sum", "addExact", "subtractExact", "multiplyExact")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_REFERENCE);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodReferenceTree methodReference = (MethodReferenceTree) tree;
    if (INVALID_METHODS.matches(methodReference) && isComparator(methodReference)) {
      reportIssue(methodReference, String.format(MESSAGE, methodReferenceName(methodReference)));
    }
  }

  private static boolean isComparator(MethodReferenceTree methodReference) {
    return methodReference.symbolType().isSubtypeOf("java.util.Comparator");
  }

  private static String methodReferenceName(MethodReferenceTree methodReference) {
    Tree expression = methodReference.expression();
    String expressionName = expression.is(Tree.Kind.MEMBER_SELECT)
      ? ((MemberSelectExpressionTree) expression).identifier().name()
      : ((IdentifierTree) expression).name();
    return expressionName + "::" + methodReference.method().name();
  }
}
