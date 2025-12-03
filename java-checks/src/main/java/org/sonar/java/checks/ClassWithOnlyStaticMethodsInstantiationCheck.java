/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2440")
public class ClassWithOnlyStaticMethodsInstantiationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeTree identifier = ((NewClassTree) tree).identifier();
    Symbol.TypeSymbol newClassTypeSymbol = identifier.symbolType().symbol();
    if (!newClassTypeSymbol.isEnum() && hasOnlyStaticMethodsAndFields(newClassTypeSymbol) && !instantiateOwnClass(identifier, newClassTypeSymbol)) {
      String message = "Remove this instantiation.";
      String name = getNewClassName(identifier);
      if (name != null) {
        message = "Remove this instantiation of \"{0}\".";
      }
      reportIssue(identifier, MessageFormat.format(message, name));
    }
  }

  private static boolean instantiateOwnClass(Tree identifier, Symbol.TypeSymbol newClassTypeSymbol) {
    Type enclosingClassType = JUtils.enclosingClass(identifier).type();
    return enclosingClassType.equals(newClassTypeSymbol.type());
  }

  private static boolean hasOnlyStaticMethodsAndFields(Symbol.TypeSymbol newClassTypeSymbol) {
    Collection<Symbol> symbols = filterMethodsAndFields(newClassTypeSymbol.memberSymbols());
    if (symbols.isEmpty()) {
      return false;
    }
    for (Symbol symbol : symbols) {
      if (!symbol.isStatic()) {
        return false;
      }
    }
    return superTypesHaveOnlyStaticMethods(newClassTypeSymbol);
  }

  private static boolean superTypesHaveOnlyStaticMethods(Symbol.TypeSymbol newClassTypeSymbol) {
    Type superClass = newClassTypeSymbol.superClass();
    if (superClass != null && !superClass.is("java.lang.Object") && !hasOnlyStaticMethodsAndFields(superClass.symbol())) {
      return false;
    }
    for (Type superInterface : newClassTypeSymbol.interfaces()) {
      if (!hasOnlyStaticMethodsAndFields(superInterface.symbol())) {
        return false;
      }
    }
    return true;
  }

  private static Collection<Symbol> filterMethodsAndFields(Collection<Symbol> symbols) {
    List<Symbol> filtered = new ArrayList<>();
    for (Symbol symbol : symbols) {
      if ((symbol.isVariableSymbol() && !isThisOrSuper(symbol)) || (symbol.isMethodSymbol() && !isConstructor(symbol))) {
        filtered.add(symbol);
      }
    }
    return filtered;
  }

  private static boolean isThisOrSuper(Symbol symbol) {
    String name = symbol.name();
    return "this".equals(name) || "super".equals(name);
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
