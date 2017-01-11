/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
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
import java.util.Collection;
import java.util.List;

@Rule(key = "S2440")
public class ClassWithOnlyStaticMethodsInstantiationCheck extends IssuableSubscriptionVisitor {
  private SemanticModel semanticModel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    semanticModel = (SemanticModel) context.getSemanticModel();
    super.scanFile(context);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.NEW_CLASS);
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

  private boolean instantiateOwnClass(Tree identifier, Symbol.TypeSymbol newClassTypeSymbol) {
    Type enclosingClassType = semanticModel.getEnclosingClass(identifier).type();
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
    List<Symbol> filtered = Lists.newArrayList();
    for (Symbol symbol : symbols) {
      if ((symbol.isVariableSymbol() && symbol.declaration() != null) || (symbol.isMethodSymbol() && !isConstructor(symbol))) {
        filtered.add(symbol);
      }
    }
    return filtered;
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
