/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "AvoidMethodWithSameTypeInArgument")
/**
 * To use subscription visitor, just extend the IssuableSubscriptionVisitor.
 */
public class MyCustomSubscriptionRule extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // Register to the kind of nodes you want to be called upon visit.
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    // Cast the node to the correct type :
    // in this case we registered only to one kind so we will only receive MethodTree see Tree.Kind enum to know about which type you can
    // cast depending on Kind.
    MethodTree methodTree = (MethodTree) tree;
    // Retrieve symbol of method.
    MethodSymbol methodSymbol = methodTree.symbol();
    Type returnType = methodSymbol.returnType().type();
    // Check method has only one argument.
    if (methodSymbol.parameterTypes().size() == 1) {
      Type argType = methodSymbol.parameterTypes().get(0);
      // Verify argument type is same as return type.
      if (argType.is(returnType.fullyQualifiedName())) {
        // raise an issue on this node of the SyntaxTree
        reportIssue(tree, "message");
      }
    }
  }
}
