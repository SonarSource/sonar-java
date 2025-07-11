/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.SpringUtils;
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
    SpringUtils.AUTOWIRED_ANNOTATION,
    "javax.inject.Inject",
    "jakarta.inject.Inject",
    "javax.annotation.Resource",
    "jakarta.annotation.Resource",
    "javax.persistence.PersistenceContext",
    "jakarta.persistence.PersistenceContext",
    SpringUtils.VALUE_ANNOTATION);

  private static final List<String> SPRING_SINGLETON_ANNOTATION = Arrays.asList(
    SpringUtils.CONTROLLER_ANNOTATION,
    SpringUtils.REST_CONTROLLER_ANNOTATION,
    SpringUtils.SERVICE_ANNOTATION,
    SpringUtils.COMPONENT_ANNOTATION,
    SpringUtils.REPOSITORY_ANNOTATION);

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
      .filter(m -> "<init>".equals(m.name()))
      .filter(m -> m.declaration() != null)
      .toList();
  }

  private static class IdentifierCollector extends BaseTreeVisitor {

    Set<Symbol> identifiers = new HashSet<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiers.add(tree.symbol());
    }
  }
}
