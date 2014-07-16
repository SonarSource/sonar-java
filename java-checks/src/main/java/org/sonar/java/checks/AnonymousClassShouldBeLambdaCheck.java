/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.api.rule.RuleKey;
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

import java.util.List;

@Rule(
    key = AnonymousClassShouldBeLambdaCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"java8"})
public class AnonymousClassShouldBeLambdaCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1604";
  private static final RuleKey RULE = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
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
    if (tree.classBody() != null) {
      List<Tree> members = tree.classBody().members();
      if (!useThisIdentifier(tree.classBody()) && !enumConstants.contains(tree.identifier()) && members.size() == 1 && members.get(0).is(Tree.Kind.METHOD)) {
        context.addIssue(tree, RULE, "Make this anonymous inner class a lambda");
      }
    }
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
      //visit the class body but ignore inner classes
      if (!visitedClassTree) {
        visitedClassTree = true;
        super.visitClass(tree);
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      //ignore anonymous classes
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      scan(tree.expression());
      //ignore identifier, because if it is this, it is a qualified this.
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      usesThisIdentifier |= "this".equals(tree.name());
    }
  }
}
