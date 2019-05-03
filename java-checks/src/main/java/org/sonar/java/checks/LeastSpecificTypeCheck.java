/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
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

@Rule(key = "S3242")
public class LeastSpecificTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (!methodSymbol.isPublic()
      || !Boolean.FALSE.equals(methodTree.isOverriding())
      || isOverloaded(methodSymbol)) {
      return;
    }

    boolean springInjectionAnnotated = isSpringInjectionAnnotated(methodSymbol.metadata());
    methodTree.parameters().stream()
      .map(VariableTree::symbol)
      .filter(p -> p.type().isClass() && !p.type().symbol().isEnum() && !p.type().is("java.lang.String"))
      .filter(p -> !(springInjectionAnnotated && p.type().is("java.util.Collection")))
      .forEach(p -> handleParameter(p, springInjectionAnnotated));
  }

  private static boolean isOverloaded(Symbol.MethodSymbol methodSymbol) {
    return ((Symbol.TypeSymbol) methodSymbol.owner()).lookupSymbols(methodSymbol.name()).size() > 1;
  }

  private void handleParameter(Symbol parameter, boolean springInjectionAnnotated) {
    Type leastSpecificType = findLeastSpecificType(parameter);
    if (parameter.type() != leastSpecificType
      && !leastSpecificType.is("java.lang.Object")) {
      String suggestedType = getSuggestedType(springInjectionAnnotated, leastSpecificType);
      reportIssue(parameter.declaration(), String.format("Use '%s' here; it is a more general type than '%s'.", suggestedType, parameter.type().name()));
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
          inheritanceGraph.update(mit.symbol());
        }
      }
    }
    return inheritanceGraph.leastSpecificType();
  }

  private static Optional<Symbol> findIteratorMethod(Symbol parameter) {
    return parameter.type().symbol().lookupSymbols("iterator").stream()
      .filter(Symbol::isMethodSymbol)
      .filter(s -> ((Symbol.MethodSymbol) s).parameterTypes().isEmpty())
      .findFirst();
  }

  private static class InheritanceGraph {

    private final Type startType;
    private List<List<Type>> chains = null;

    private InheritanceGraph(Type type) {
      startType = type;
    }

    private void update(Symbol m) {
      if (chains == null) {
        chains = computeChains(m, startType);
      } else {
        refineChains(m);
      }
    }

    private List<List<Type>> computeChains(Symbol m, Type type) {
      Symbol.TypeSymbol typeSymbol = type.symbol();
      Set<ClassJavaType> superTypes = ((JavaSymbol.TypeJavaSymbol) typeSymbol).directSuperTypes();
      List<List<Type>> result = new ArrayList<>();
      for (ClassJavaType superType : superTypes) {
        for (List<Type> chain : computeChains(m, superType)) {
          chain.add(type);
          result.add(chain);
        }
      }

      boolean definesSymbol = definesSymbol(m, typeSymbol);
      boolean isSpecialization = !((JavaType) startType).isParameterized() && ((JavaType) type).isParameterized();
      if (definesSymbol && !isSpecialization && result.isEmpty()) {
        result.add(Lists.newArrayList(type));
      }
      return result;
    }

    private static boolean definesOrInheritsSymbol(Symbol symbol, JavaSymbol.TypeJavaSymbol typeSymbol) {
      return definesSymbol(symbol, typeSymbol)
        || typeSymbol.superTypes().stream().anyMatch(superType -> definesSymbol(symbol, superType.symbol()));
    }

    private static boolean definesSymbol(Symbol m, Symbol.TypeSymbol typeSymbol) {
      return typeSymbol.memberSymbols().stream().anyMatch(s -> isOverriding(m, s, ((ClassJavaType) typeSymbol.type())));
    }

    private void refineChains(Symbol m) {
      for (List<Type> chain : chains) {
        Iterator<Type> chainIterator = chain.iterator();
        while (chainIterator.hasNext()) {
          Type type = chainIterator.next();
          if (definesOrInheritsSymbol(m, (JavaSymbol.TypeJavaSymbol) type.symbol())) {
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

    private static boolean isOverriding(Symbol s1, Symbol s2, ClassJavaType superClass) {
      return s1.isMethodSymbol() && s2.isMethodSymbol() && isOverridingMethod(((JavaSymbol.MethodJavaSymbol) s1), ((JavaSymbol.MethodJavaSymbol) s2), superClass);
    }

    private static boolean isOverridingMethod(JavaSymbol.MethodJavaSymbol s1, JavaSymbol.MethodJavaSymbol s2, ClassJavaType superClass) {
      return s1.name().equals(s2.name()) && BooleanUtils.isTrue(s1.checkOverridingParameters(s2, superClass));
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
    return metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
      || metadata.isAnnotatedWith("javax.inject.Inject")
      || metadata.isAnnotatedWith("javax.annotation.Resource");
  }
}
