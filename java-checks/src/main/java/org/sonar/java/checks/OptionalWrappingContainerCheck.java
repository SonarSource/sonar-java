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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S9404")
public class OptionalWrappingContainerCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
  private static final String JAVA_UTIL_MAP = "java.util.Map";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    TypeTree returnTypeTree = methodTree.returnType();
    if (returnTypeTree == null || !returnTypeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return;
    }
    if (!Boolean.FALSE.equals(methodTree.isOverriding())) {
      return;
    }

    Type returnType = returnTypeTree.symbolType();
    if (!returnType.is(JAVA_UTIL_OPTIONAL) || !returnType.isParameterized() || returnType.typeArguments().size() != 1) {
      return;
    }

    Type wrappedType = returnType.typeArguments().get(0);
    String containerType = containerType(wrappedType);
    if (containerType != null) {
      reportIssue(((ParameterizedTypeTree) returnTypeTree).type(), "Return an empty " + containerType + " instead of wrapping it in Optional.");
    }
  }

  @Nullable
  private static String containerType(Type type) {
    if (type.isArray()) {
      return "array";
    }
    if (type.isSubtypeOf(JAVA_UTIL_COLLECTION)) {
      return "collection";
    }
    if (type.isSubtypeOf(JAVA_UTIL_MAP)) {
      return "map";
    }
    return null;
  }
}
