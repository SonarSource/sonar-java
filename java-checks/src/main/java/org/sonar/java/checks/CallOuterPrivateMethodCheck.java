/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S3398",
  name = "\"private\" methods called only by inner classes should be moved to those classes",
  priority = Priority.MAJOR,
  tags = {Tag.CONFUSING})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class CallOuterPrivateMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isInnerClass(classSymbol)) {
      MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor(classSymbol);
      classTree.accept(methodInvocationVisitor);
      methodInvocationVisitor.checkUsages();
    }
  }

  private static boolean isInnerClass(Symbol symbol) {
    return symbol.owner().isTypeSymbol();
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {
    private final Symbol.TypeSymbol classSymbol;
    private final Multiset<Symbol> usages = HashMultiset.create();

    public MethodInvocationVisitor(Symbol.TypeSymbol classSymbol) {
      this.classSymbol = classSymbol;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol symbol = tree.symbol();
      if (isPrivateMethodOfOuterClass(symbol)) {
        usages.add(symbol);
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isPrivateMethodOfOuterClass(Symbol symbol) {
      return symbol.isPrivate() && symbol.owner().equals(classSymbol.owner());
    }

    public void checkUsages() {
      for (Symbol methodUsed : usages.elementSet()) {
        if (methodUsed.usages().size() == usages.count(methodUsed)) {
          reportIssueOnMethod((MethodTree) methodUsed.declaration());
        }
      }
    }

    private void reportIssueOnMethod(@Nullable MethodTree declaration) {
      if (declaration != null) {
        String message = "Move this method into ";
        if (classSymbol.name().isEmpty()) {
          message += "the anonymous class declared at line " + ((JavaTree) classSymbol.declaration()).getLine()+".";
        } else {
          message += "\"" + classSymbol.name() + "\".";
        }
        reportIssue(declaration.simpleName(), message);
      }
    }

  }
}
