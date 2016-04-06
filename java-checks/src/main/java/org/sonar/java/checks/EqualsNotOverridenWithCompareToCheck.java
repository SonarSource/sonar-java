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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1210",
  name = "\"equals(Object obj)\" should be overridden along with the \"compareTo(T obj)\" method",
  priority = Priority.CRITICAL,
  tags = {Tag.BUG})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("15min")
public class EqualsNotOverridenWithCompareToCheck extends IssuableSubscriptionVisitor {

    private static final List<String> EXCLUDED_ANNOTATIONS_TYPE = ImmutableList.<String>builder()
            .add("lombok.EqualsAndHashCode")
            .add("lombok.Data")
            .add("lombok.Value")
            .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isComparable(classTree)) {
      MethodTree compare = null;

      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.METHOD)) {
          MethodTree method = (MethodTree) member;
          if (isCompareToMethod(method)) {
            compare = method;
          }
        }
      }

      if (compare != null && !generatesEquals(classTree) && !implementsEquals(classTree)) {
        reportIssue(compare.simpleName(), "Override \"equals(Object obj)\" to comply with the contract of the \"compareTo(T o)\" method.");
      }
    }
  }

  private static boolean implementsEquals(ClassTree classTree) {
      return hasNotFinalEqualsMethod(classTree.symbol());
    }

  private static boolean isCompareToMethod(MethodTree method) {
    String name = method.simpleName().name();
    return "compareTo".equals(name) && returnsInt(method) && method.parameters().size() == 1;
  }

  private static boolean isEqualsMethod(Symbol symbol) {
      if (symbol.isMethodSymbol()) {
        List<Type> parameterTypes = ((Symbol.MethodSymbol) symbol).parameterTypes();
        return !parameterTypes.isEmpty() && parameterTypes.get(0).is("java.lang.Object");
      }
      return false;
    }

  private static boolean hasNotFinalEqualsMethod(Symbol.TypeSymbol superClassSymbol) {
      for (Symbol symbol : superClassSymbol.lookupSymbols("equals")) {
        if (isEqualsMethod(symbol) && !symbol.isFinal()) {
          return true;
        }
      }
      return false;
    }

  private static boolean generatesEquals(ClassTree classTree) {
      SymbolMetadata metadata = classTree.symbol().metadata();
      for (String annotation : EXCLUDED_ANNOTATIONS_TYPE) {
        if (metadata.isAnnotatedWith(annotation)) {
          return true;
        }
      }
      return false;
    }

  private static boolean isComparable(ClassTree tree) {
    for (Type type : tree.symbol().interfaces()) {
      if (type.is("java.lang.Comparable")) {
        return true;
      }
    }
    return false;
  }

  private static boolean returnsInt(MethodTree tree) {
    TypeTree typeTree = tree.returnType();
    return typeTree != null && typeTree.symbolType().isPrimitive(Type.Primitives.INT);
  }

}
