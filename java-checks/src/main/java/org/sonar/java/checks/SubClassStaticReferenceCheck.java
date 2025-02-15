/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2390")
public class SubClassStaticReferenceCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Type classType = classTree.symbol().type();
    List<Tree> members = classTree.members();

    // JLS 12.4. Initialization of Classes and Interfaces:
    // Initialization of a class consists of executing its static initializers and the initializers for static fields (class variables)
    // declared in the class.
    checkStaticVariables(members, classType);
    checkStaticInitializers(members, classType);
  }

  private void checkStaticVariables(List<Tree> members, Type classType) {
    members.stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(SubClassStaticReferenceCheck::isStaticVariable)
      .map(VariableTree::initializer)
      .filter(Objects::nonNull)
      .forEach(initializer -> initializer.accept(new StaticAccessVisitor(classType)));
  }

  private static boolean isStaticVariable(VariableTree tree) {
    return tree.symbol().isStatic();
  }

  private void checkStaticInitializers(List<Tree> members, Type classType) {
    members.stream()
      .filter(member -> member.is(Tree.Kind.STATIC_INITIALIZER))
      .forEach(tree -> tree.accept(new StaticAccessVisitor(classType)));
  }


  private class StaticAccessVisitor extends BaseTreeVisitor {
    private final Type classTypeErasure;

    public StaticAccessVisitor(Type classType) {
      this.classTypeErasure = classType.erasure();
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      // skip the variable
      scan(tree.expression());
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip anonymous classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      if (JavaKeyword.CLASS.getValue().equals(tree.identifier().name())) {
        // skip visit of class literal (MyType.class)
        return;
      }
      super.visitMemberSelectExpression(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Type type = tree.symbolType();
      if (!sameErasure(type) && type.isSubtypeOf(classTypeErasure) && !isNestedSubtype(type)) {
        reportIssue(tree, String.format("Remove this reference to \"%s\".", type.symbol().name()));
      }
    }

    private boolean sameErasure(Type type) {
      return classTypeErasure.equals(type.erasure());
    }

    private boolean isNestedSubtype(Type type) {
      // The owner cannot be null in this context thanks to the checks in visitIdentifier.
      Type ownerType = Objects.requireNonNull(type.symbol().owner()).type();
      return ownerType != null && ownerType.erasure().isSubtypeOf(classTypeErasure);
    }

  }

}
