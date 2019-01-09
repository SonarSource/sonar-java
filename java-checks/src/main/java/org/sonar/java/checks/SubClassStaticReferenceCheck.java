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

import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.resolve.MethodJavaType;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Rule(key = "S2390")
public class SubClassStaticReferenceCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
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
    private final Type classType;

    public StaticAccessVisitor(Type classType) {
      this.classType = classType;
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
      if (type instanceof MethodJavaType) {
        type = ((MethodJavaType) type).resultType();
      }
      if (!sameErasure(type) && type.isSubtypeOf(classType.erasure())) {
        reportIssue(tree, String.format("Remove this reference to \"%s\".", type.symbol().name()));
      }
    }

    private boolean sameErasure(Type type) {
      return classType.erasure().equals(type.erasure());
    }
  }

}
