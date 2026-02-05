/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Checks that the "main" method has the correct signature for a program entry point.
 *
 * <p>Note, that even with a correct signature, the "main" method may not be valid entry point.
 * For example, it may be declared in an abstract class or an interface.
 */
@Rule(key = "S3051")
public class MainMethodSignatureCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "\"main\" method should only be used for the program entry point and should have appropriate signature.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!"main".equals(methodTree.simpleName().name())) {
      return;
    }
    if (Boolean.FALSE.equals(isValidMainSignature(methodTree))) {
      reportIssue(methodTree.simpleName(), MESSAGE);
    }
  }

  /**
   * Checks if the signature is a valid "main" method signature in Java 25.
   *
   * @return true, false, or null if it cannot be determined
   */
  private static Boolean isValidMainSignature(MethodTree methodTree) {
    TypeTree returnType = methodTree.returnType();
    if (returnType == null) {
      return null;
    }
    if (!returnType.symbolType().isVoid()) {
      return false;
    }

    List<VariableTree> parameters = methodTree.parameters();
    if (parameters.isEmpty()) {
      return true;
    }
    if (parameters.size() != 1) {
      return false;
    }

    TypeTree paramType = parameters.get(0).type();
    return paramType instanceof ArrayTypeTree att
      && "String".equals(att.type().symbolType().name());
  }
}
