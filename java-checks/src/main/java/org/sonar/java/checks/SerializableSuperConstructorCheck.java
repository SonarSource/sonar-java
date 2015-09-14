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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

@Rule(
  key = "S2055",
  name = "The non-serializable super class of a \"Serializable\" class must have a no-argument constructor",
  tags = {"bug", "serialization"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("30min")
public class SerializableSuperConstructorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      Symbol.TypeSymbol classSymbol = ((ClassTree) tree).symbol();
      Type superclass = classSymbol.superClass();
      if (isSerializable(classSymbol.type()) && isNotSerializableMissingNoArgConstructor(superclass)) {
        addIssue(tree, "Add a no-arg constructor to \"" + superclass + "\".");
      }
    }
  }

  private static boolean isNotSerializableMissingNoArgConstructor(@Nullable Type superclass) {
    return superclass != null && !isSerializable(superclass) && !hasNonPrivateNoArgConstructor(superclass);
  }

  private static boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean hasNonPrivateNoArgConstructor(Type type) {
    Collection<Symbol> constructors = type.symbol().lookupSymbols("<init>");
    for (Symbol member : constructors) {
      if (member.isMethodSymbol()) {
        Symbol.MethodSymbol method = (Symbol.MethodSymbol) member;
        if (method.parameterTypes().isEmpty() && !method.isPrivate()) {
          return true;
        }
      }
    }
    return constructors.isEmpty();
  }

}
