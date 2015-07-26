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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1172",
  name = "Unused method parameters should be removed",
  tags = {"misra", "unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class UnusedMethodParameterCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && methodTree.block() != null && !isExcluded(methodTree)) {
      List<String> unused = Lists.newArrayList();
      for (VariableTree var : methodTree.parameters()) {
        if (var.symbol().usages().isEmpty()) {
          unused.add(var.simpleName().name());
        }
      }
      if (!unused.isEmpty()) {
        addIssue(methodTree, "Remove the unused method parameter(s) \"" + Joiner.on(",").join(unused) + "\".");
      }
    }
  }

  private static boolean isExcluded(MethodTree tree) {
    return ((MethodTreeImpl) tree).isMainMethod() || isOverriding(tree) || isSerializableMethod(tree) || isDesignedForExtension(tree);
  }

  private static boolean isDesignedForExtension(MethodTree tree) {
    return !ModifiersUtils.hasModifier(tree.modifiers(), Modifier.PRIVATE) && isEmptyOrThrowStatement(tree.block());
  }

  private static boolean isEmptyOrThrowStatement(BlockTree block) {
    return block.body().isEmpty() || (block.body().size() == 1 && block.body().get(0).is(Tree.Kind.THROW_STATEMENT));
  }

  private static boolean isSerializableMethod(MethodTree methodTree) {
    boolean result = false;
    // FIXME detect methods based on type of arg and throws, not arity.
    if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE) && methodTree.parameters().size() == 1) {
      result |= "writeObject".equals(methodTree.simpleName().name()) && methodTree.throwsClauses().size() == 1;
      result |= "readObject".equals(methodTree.simpleName().name()) && methodTree.throwsClauses().size() == 2;
    }
    return result;
  }

  private static boolean isOverriding(MethodTree tree) {
    // if overriding cannot be determined, we consider it is overriding to avoid FP.
    return !BooleanUtils.isFalse(((MethodTreeImpl) tree).isOverriding());
  }
}
