/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1604",
  name = "Anonymous inner classes containing only one method should become lambdas",
  tags = {"java8"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class AnonymousClassShouldBeLambdaCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private List<IdentifierTree> enumConstants;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    enumConstants = Lists.newArrayList();
    scan(context.getTree());
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    enumConstants.add(tree.simpleName());
    super.visitEnumConstant(tree);
    enumConstants.remove(tree.simpleName());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    super.visitNewClass(tree);
    ClassTree classBody = tree.classBody();
    if (classBody != null) {
      TypeTree identifier = tree.identifier();
      if (!useThisIdentifier(classBody) && !enumConstants.contains(identifier) && hasOnlyOneMethod(classBody.members())) {
        context.addIssue(identifier, this, "Make this anonymous inner class a lambda");
      }
    }
  }

  private static boolean hasOnlyOneMethod(List<Tree> members) {
    int methodCounter = 0;
    for (Tree tree : members) {
      if (!tree.is(Tree.Kind.EMPTY_STATEMENT, Tree.Kind.METHOD)) {
        return false;
      } else if (tree.is(Tree.Kind.METHOD)) {
        methodCounter++;
      }
    }
    return methodCounter == 1;
  }

  private boolean useThisIdentifier(ClassTree body) {
    ThisIdentifierVisitor visitor = new ThisIdentifierVisitor();
    body.accept(visitor);
    return visitor.usesThisIdentifier;
  }

  private static class ThisIdentifierVisitor extends BaseTreeVisitor {
    boolean usesThisIdentifier = false;
    boolean visitedClassTree = false;

    @Override
    public void visitClass(ClassTree tree) {
      // visit the class body but ignore inner classes
      if (!visitedClassTree) {
        visitedClassTree = true;
        super.visitClass(tree);
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // ignore anonymous classes
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      scan(tree.expression());
      // ignore identifier, because if it is this, it is a qualified this.
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      usesThisIdentifier |= "this".equals(tree.name());
    }
  }

}
