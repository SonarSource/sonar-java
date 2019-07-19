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
package org.sonar.java.checks.spring;

import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3749")
public class SpringComponentWithNonAutowiredMembersCheck extends IssuableSubscriptionVisitor {

  @RuleProperty(
    key = "customInjectionAnnotations",
    description = "comma-separated list of FQDN annotation names to consider as valid",
    defaultValue = "")
  public String customInjectionAnnotations = "";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;
    SymbolMetadata clazzMeta = clazzTree.symbol().metadata();
    Set<Symbol> symbolsUsedInConstructors = symbolsUsedInConstructors(clazzTree);

    if (isSpringComponent(clazzMeta)) {
      clazzTree.members().stream().filter(v -> v.is(Kind.VARIABLE))
        .map(m -> (VariableTree) m)
        .filter(v -> !v.symbol().isStatic())
        .filter(v -> !isSpringInjectionAnnotated(v.symbol().metadata()))
        .filter(v -> !isCustomInjectionAnnotated(v.symbol().metadata()))
        .filter(v -> !symbolsUsedInConstructors.contains(v.symbol()))
        .forEach(v -> reportIssue(v.simpleName(), "Annotate this member with \"@Autowired\", \"@Resource\", \"@Inject\", or \"@Value\", or remove it."));
    }
  }

  private static boolean isSpringInjectionAnnotated(SymbolMetadata metadata) {
    return metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
      || metadata.isAnnotatedWith("javax.inject.Inject")
      || metadata.isAnnotatedWith("javax.annotation.Resource")
      || metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Value");
  }

  private boolean isCustomInjectionAnnotated(SymbolMetadata metadata) {
    return Splitter.on(",").trimResults()
      .splitToList(customInjectionAnnotations)
      .stream()
      .anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean isSpringComponent(SymbolMetadata clazzMeta) {
    return clazzMeta.isAnnotatedWith("org.springframework.stereotype.Controller")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Service")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Repository");
  }

  private Set<Symbol> symbolsUsedInConstructors(ClassTree clazzTree) {
    List<Symbol.MethodSymbol> constructors = constructors(clazzTree);
    return constructors.stream()
      .filter(ctor -> isAutowired(constructors, ctor))
      .map(Symbol.MethodSymbol::declaration)
      .flatMap(ctorTree -> symbolsUsedInMethod(ctorTree.symbol()).stream())
      .collect(Collectors.toSet());
  }

  private boolean isAutowired(List<Symbol.MethodSymbol> constructors, Symbol.MethodSymbol ctor) {
    return isSpringInjectionAnnotated(ctor.metadata())
      || isCustomInjectionAnnotated(ctor.metadata()) || constructors.size() == 1;
  }

  private Set<Symbol> symbolsUsedInMethod(Symbol.MethodSymbol methodSymbol) {
    IdentifierCollector identifierCollector = new IdentifierCollector();
    methodSymbol.declaration().accept(identifierCollector);
    return identifierCollector.identifiers;
  }

  private static List<Symbol.MethodSymbol> constructors(ClassTree clazzTree) {
    return clazzTree.symbol().memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .map(s -> (Symbol.MethodSymbol) s)
      .filter(m -> m.name().equals("<init>"))
      .filter(m -> m.declaration() != null)
      .collect(Collectors.toList());
  }

  private static class IdentifierCollector extends BaseTreeVisitor {

    Set<Symbol> identifiers = new HashSet<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiers.add(tree.symbol());
    }
  }
}
