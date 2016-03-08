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
package org.sonar.java.checks.unused;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodInvocationMatcherCollection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(
  key = "S1172",
  name = "Unused method parameters should be removed",
  priority = Priority.MAJOR,
  tags = {Tag.MISRA, Tag.UNUSED})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class UnusedMethodParameterCheck extends IssuableSubscriptionVisitor {

  private static final String AUTHORIZED_ANNOTATION = "javax.enterprise.event.Observes";
  private static final MethodInvocationMatcherCollection SERIALIZABLE_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().name("writeObject").addParameter("java.io.ObjectOutputStream"),
    MethodMatcher.create().name("readObject").addParameter("java.io.ObjectInputStream"));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && methodTree.block() != null && !isExcluded(methodTree)) {
      List<IdentifierTree> unused = Lists.newArrayList();
      for (VariableTree var : methodTree.parameters()) {
        Symbol symbol = var.symbol();
        if (symbol.usages().isEmpty() && !symbol.metadata().isAnnotatedWith(AUTHORIZED_ANNOTATION)) {
          unused.add(var.simpleName());
        }
      }
      if (!unused.isEmpty()) {
        List<JavaFileScannerContext.Location> locations = new ArrayList<>();
        for (IdentifierTree identifier : unused.subList(1, unused.size())) {
          locations.add(new JavaFileScannerContext.Location("Remove this unused method parameter "+identifier.name()+"\".", identifier));
        }
        IdentifierTree firstUnused = unused.get(0);
        reportIssue(firstUnused, "Remove this unused method parameter \"" + firstUnused.name() + "\".", locations, null);
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
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE) && SERIALIZABLE_METHODS.anyMatch(methodTree);
  }

  private static boolean isOverriding(MethodTree tree) {
    // if overriding cannot be determined, we consider it is overriding to avoid FP.
    return !BooleanUtils.isFalse(((MethodTreeImpl) tree).isOverriding());
  }
}
