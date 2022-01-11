/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Arrays;
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
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.SpringUtils.isScopeSingleton;

@Rule(key = "S3749")
public class SpringComponentWithNonAutowiredMembersCheck extends IssuableSubscriptionVisitor {

  @RuleProperty(
    key = "customInjectionAnnotations",
    description = "comma-separated list of FQDN annotation names to consider as valid",
    defaultValue = "")
  public String customInjectionAnnotations = "";

  private static final List<String> SPRING_INJECTION_ANNOTATION = Arrays.asList(
    "org.springframework.beans.factory.annotation.Autowired",
    "javax.inject.Inject",
    "javax.annotation.Resource",
    "javax.persistence.PersistenceContext",
    "org.springframework.beans.factory.annotation.Value");

  private static final List<String> SPRING_SINGLETON_ANNOTATION = Arrays.asList(
    "org.springframework.stereotype.Controller",
    "org.springframework.stereotype.Service",
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Repository");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;
    Set<Symbol> symbolsUsedInConstructors = symbolsUsedInConstructors(clazzTree);

    if (isSpringSingletonComponent(clazzTree.symbol().metadata())) {
      clazzTree.members().stream().filter(v -> v.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .filter(v -> !v.symbol().isStatic())
        .filter(v -> !isSpringInjectionAnnotated(v.symbol().metadata()))
        .filter(v -> !isCustomInjectionAnnotated(v.symbol().metadata()))
        .filter(v -> !symbolsUsedInConstructors.contains(v.symbol()))
        .forEach(v -> reportIssue(v.simpleName(), "Annotate this member with \"@Autowired\", \"@Resource\", \"@Inject\", or \"@Value\", or remove it."));
    }
  }

  private static boolean isSpringInjectionAnnotated(SymbolMetadata metadata) {
    return SPRING_INJECTION_ANNOTATION.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private boolean isCustomInjectionAnnotated(SymbolMetadata metadata) {
    return Arrays.stream(customInjectionAnnotations.split(","))
      .map(String::trim)
      .anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean isSpringSingletonComponent(SymbolMetadata clazzMeta) {
    return SPRING_SINGLETON_ANNOTATION.stream().anyMatch(clazzMeta::isAnnotatedWith)
      && !isUsingConfigurationProperties(clazzMeta)
      && isScopeSingleton(clazzMeta);
  }

  private static boolean isUsingConfigurationProperties(SymbolMetadata classMeta) {
    return classMeta.isAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties");
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

  private static Set<Symbol> symbolsUsedInMethod(Symbol.MethodSymbol methodSymbol) {
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
