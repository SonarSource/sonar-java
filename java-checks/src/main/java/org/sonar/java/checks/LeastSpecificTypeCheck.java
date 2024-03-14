/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.AnnotationsHelper.hasUnknownAnnotation;

@Rule(key = "S3242")
public class LeastSpecificTypeCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> SPRING_INJECT_ANNOTATIONS = Set.of(
    "org.springframework.beans.factory.annotation.Autowired",
    "javax.inject.Inject",
    "jakarta.inject.Inject",
    "javax.annotation.Resource",
    "jakarta.annotation.Resource");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    SymbolMetadata metadata = methodSymbol.metadata();
    if (!methodSymbol.isPublic()
      || !Boolean.FALSE.equals(methodTree.isOverriding())
      || isOverloaded(methodSymbol)
      || hasUnknownAnnotation(metadata)) {
      return;
    }

    boolean springInjectionAnnotated = isSpringInjectionAnnotated(metadata);
    methodTree.parameters().stream()
      .map(VariableTree::symbol)
      .filter(p -> p.type().isClass() && !p.type().symbol().isEnum() && !isStringType(p.type()))
      .filter(p -> !(springInjectionAnnotated && p.type().is("java.util.Collection")))
      .forEach(p -> handleParameter(p, springInjectionAnnotated));
  }

  private static boolean isOverloaded(Symbol.MethodSymbol methodSymbol) {
    return ((Symbol.TypeSymbol) methodSymbol.owner()).lookupSymbols(methodSymbol.name()).size() > 1;
  }

  private static boolean isStringType(Type type) {
    return type.isUnknown() || type.is("java.lang.String");
  }

  private void handleParameter(Symbol parameter, boolean springInjectionAnnotated) {
    Type parameterType = parameter.type();
    if (parameterType.symbol().metadata().isAnnotatedWith("java.lang.FunctionalInterface")) {
      // Exclude functional interface, it's wrong to have issues on UnaryOperator<T> and ask the user to use Function<T,T> instead
      return;
    }

    Type leastSpecificType = findLeastSpecificType(parameter);
    if (parameterType != leastSpecificType
      && !leastSpecificType.is("java.lang.Object")) {

      String suggestedType = getSuggestedType(springInjectionAnnotated, leastSpecificType);
      String message = String.format("Use '%s' here; it is a more general type than '%s'.", suggestedType, parameterType.erasure().name());
      reportIssue(parameter.declaration(), message);
    }
  }

  private static String getSuggestedType(boolean springInjectionAnnotated, Type leastSpecificType) {
    if (springInjectionAnnotated && leastSpecificType.is("java.lang.Iterable")) {
      return "java.util.Collection";
    }
    return leastSpecificType.fullyQualifiedName().replace('$', '.');
  }

  private static Type findLeastSpecificType(Symbol parameter) {
    InheritanceGraph inheritanceGraph = new InheritanceGraph(parameter.type());
    for (IdentifierTree usage : parameter.usages()) {
      Tree parent = usage.parent();
      while (parent != null && !parent.is(Tree.Kind.ARGUMENTS, Tree.Kind.METHOD_INVOCATION, Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT)) {
        parent = parent.parent();
      }
      if (parent == null || parent.is(Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.ARGUMENTS)) {
        return parameter.type();
      }
      if (parent.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        findIteratorMethod(parameter).ifPresent(inheritanceGraph::update);
      } else if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) parent;
        if (isMethodInvocationOnParameter(parameter, mit)) {
          inheritanceGraph.update(mit.methodSymbol());
        }
      }
    }
    return inheritanceGraph.leastSpecificType();
  }

  private static Optional<Symbol.MethodSymbol> findIteratorMethod(Symbol parameter) {
    return parameter.type().symbol().lookupSymbols("iterator").stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast)
      .filter(methodSymbol -> methodSymbol.parameterTypes().isEmpty())
      .findFirst();
  }

  private static class InheritanceGraph {

    private final Type startType;
    private List<List<Type>> chains = null;

    private InheritanceGraph(Type type) {
      startType = type;
    }

    private void update(Symbol.MethodSymbol m) {
      if (chains == null) {
        chains = computeChains(m, startType);
      } else {
        refineChains(m);
      }
    }

    private List<List<Type>> computeChains(Symbol.MethodSymbol m, Type type) {
      List<List<Type>> result = new ArrayList<>();
      Symbol.TypeSymbol typeSymbol = type.symbol();
      Type superClass = typeSymbol.superClass();
      if (superClass != null) {
        computeChainsForSuperType(result, m, type, superClass);
      }
      for (Type i : typeSymbol.interfaces()) {
        computeChainsForSuperType(result, m, type, i);
      }

      boolean definesSymbol = definesSymbol(m, typeSymbol);
      boolean isSpecialization = !startType.isParameterized() && type.isParameterized();
      if (definesSymbol && !isSpecialization && result.isEmpty()) {
        ArrayList<Type> list = new ArrayList<>();
        list.add(type);
        result.add(list);
      }
      return result;
    }

    private void computeChainsForSuperType(List<List<Type>> result, Symbol.MethodSymbol methodSymbol, Type type, Type superType) {
      for (List<Type> chain : computeChains(methodSymbol, superType)) {
        chain.add(type);
        result.add(chain);
      }
    }

    private static boolean definesOrInheritsSymbol(Symbol.MethodSymbol methodSymbol, Symbol.TypeSymbol typeSymbol) {
      return definesSymbol(methodSymbol, typeSymbol)
        || typeSymbol.superTypes().stream().anyMatch(superType -> definesSymbol(methodSymbol, superType.symbol()));
    }

    private static boolean definesSymbol(Symbol.MethodSymbol methodSymbol, Symbol.TypeSymbol typeSymbol) {
      return typeSymbol.memberSymbols()
        .stream()
        .filter(Symbol::isMethodSymbol)
        .map(Symbol.MethodSymbol.class::cast)
        .anyMatch(memberMethodSymbol -> isOverridingWithSameReturnType(methodSymbol, memberMethodSymbol));
    }

    private void refineChains(Symbol.MethodSymbol m) {
      for (List<Type> chain : chains) {
        Iterator<Type> chainIterator = chain.iterator();
        while (chainIterator.hasNext()) {
          Type type = chainIterator.next();
          if (definesOrInheritsSymbol(m, type.symbol())) {
            break;
          }
          chainIterator.remove();
        }
      }
    }

    private Type leastSpecificType() {
      if (chains == null) {
        return startType;
      }
      chains.forEach(c -> c.removeIf(t -> !t.symbol().isPublic()));
      if (chains.stream().allMatch(List::isEmpty)) {
        return startType;
      }
      // pick longest chain, if multiple have same length, prefer interface, if multiple choices, choose alphabetically
      chains.sort(Comparator.<List<Type>>comparingInt(List::size)
        .thenComparing(chain -> chain.get(0).symbol().isInterface(), Boolean::compare)
        .thenComparing(chain -> chain.get(0), Comparator.comparing(Type::fullyQualifiedName).reversed())
        .reversed());

      List<Type> longestChain = chains.get(0);
      return longestChain.get(0);
    }

    private static boolean isOverridingWithSameReturnType(Symbol.MethodSymbol m, Symbol.MethodSymbol leastSpecificM) {
      return m.name().equals(leastSpecificM.name())
        && (m.returnType().type().erasure() == leastSpecificM.returnType().type().erasure())
        && ConfusingOverloadCheck.isPotentialOverride(m, leastSpecificM);
    }

    private static boolean isGenericReturnType(Symbol.MethodSymbol m) {
      return m.returnType().type().isTypeVar() || m.returnType().type().typeArguments().stream().anyMatch(Type::isTypeVar);
    }
  }

  private static boolean isMethodInvocationOnParameter(Symbol parameter, MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) mit.methodSelect();
      if (mset.expression().is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) mset.expression()).symbol();
        return symbol == parameter;
      }
    }
    return false;
  }

  private static boolean isSpringInjectionAnnotated(SymbolMetadata metadata) {
    return SPRING_INJECT_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }
}
