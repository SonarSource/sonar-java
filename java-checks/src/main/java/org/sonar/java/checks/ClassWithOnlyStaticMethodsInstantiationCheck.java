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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

@Rule(
  key = "S2440",
  name = "Classes with only \"static\" methods should not be instantiated",
  tags = {"clumsy"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("2min")
public class ClassWithOnlyStaticMethodsInstantiationCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeTree identifier = ((NewClassTree) tree).identifier();
    Symbol.TypeSymbol newClassTypeSymbol = identifier.symbolType().symbol();
    if (!newClassTypeSymbol.isEnum() && hasOnlyStaticMethods(newClassTypeSymbol) && !instantiateOwnClass(identifier, newClassTypeSymbol)) {
      String message = "Remove this instantiation.";
      String name = getNewClassName(identifier);
      if (name != null) {
        message = "Remove this instantiation of \"{0}\".";
      }
      addIssue(tree, MessageFormat.format(message, name));
    }
  }

  private boolean instantiateOwnClass(Tree identifier, Symbol.TypeSymbol newClassTypeSymbol) {
    Type enclosingClassType = getSemanticModel().getEnclosingClass(identifier).type();
    return enclosingClassType.equals(newClassTypeSymbol.type());
  }

  private static boolean hasOnlyStaticMethods(Symbol.TypeSymbol newClassTypeSymbol) {
    Collection<MethodSymbol> methods = filterMethods(newClassTypeSymbol.memberSymbols());
    if (methods.isEmpty()) {
      return false;
    }
    for (MethodSymbol method : methods) {
      if (!method.isStatic()) {
        return false;
      }
    }
    return superClassHasOnlyStaticMethods(newClassTypeSymbol);
  }

  private static boolean superClassHasOnlyStaticMethods(Symbol.TypeSymbol newClassTypeSymbol) {
    Type superClass = newClassTypeSymbol.superClass();
    if (superClass != null && !superClass.is("java.lang.Object")) {
      return hasOnlyStaticMethods(superClass.symbol());
    }
    return true;
  }

  private static Collection<MethodSymbol> filterMethods(Collection<Symbol> symbols) {
    List<MethodSymbol> methods = Lists.newArrayList();
    for (Symbol symbol : symbols) {
      if (symbol.isMethodSymbol() && !isConstructor(symbol)) {
        methods.add((MethodSymbol) symbol);
      }
    }
    return methods;
  }

  private static boolean isConstructor(Symbol symbol) {
    return "<init>".equals(symbol.name());
  }

  @Nullable
  private static String getNewClassName(Tree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    } else if (tree.is(Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) tree).identifier().name();
    } else if (tree.is(Kind.PARAMETERIZED_TYPE)) {
      return getNewClassName(((ParameterizedTypeTree) tree).type());
    }
    return null;
  }
}
