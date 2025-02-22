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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.AbstractAssertionVisitor;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.sonar.java.checks.helpers.UnitTestUtils.isUnitTest;

@Rule(key = "S2699")
public class AssertionsInTestsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Logger LOG = LoggerFactory.getLogger(AssertionsInTestsCheck.class);

  @RuleProperty(
    key = "customAssertionMethods",
    description = "Comma-separated list of fully qualified method symbols that should be considered as assertion methods. " +
      "The wildcard character can be used at the end of the method name.",
    defaultValue = "")
  public String customAssertionMethods = "";
  private MethodMatchers customAssertionMethodsMatcher = null;

  private final Map<Symbol, Boolean> assertionInMethod = new HashMap<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      // requires semantic
      return;
    }
    this.context = context;
    assertionInMethod.clear();
    scan(context.getTree());
    assertionInMethod.clear();
  }

  @Override
  public void visitMethod(MethodTree methodTree) {
    if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.ABSTRACT)) {
      return;
    }

    if (isUnitTest(methodTree) && !isSpringBootSanityTest(methodTree) && !expectAssertion(methodTree) && !isLocalMethodWithAssertion(methodTree.symbol())) {
      context.reportIssue(this, methodTree.simpleName(), "Add at least one assertion to this test case.");
    }
  }

  private static boolean isSpringBootSanityTest(MethodTree methodTree){
    if("contextLoads".equals(methodTree.simpleName().name())){
      ClassTree classTree = (ClassTree) methodTree.parent();
      return classTree.symbol().metadata().isAnnotatedWith("org.springframework.boot.test.context.SpringBootTest");
    }
    return false;
  }

  private boolean isLocalMethodWithAssertion(Symbol symbol) {
    if (!assertionInMethod.containsKey(symbol)) {
      assertionInMethod.put(symbol, false);
      Tree declaration = symbol.declaration();
      if (declaration != null) {
        AssertionVisitor assertionVisitor = new AssertionVisitor(getCustomAssertionMethodsMatcher());
        declaration.accept(assertionVisitor);
        assertionInMethod.put(symbol, assertionVisitor.hasAssertion());
      }
    }

    return assertionInMethod.get(symbol);
  }

  private MethodMatchers getCustomAssertionMethodsMatcher() {
    if (customAssertionMethodsMatcher == null) {
      String[] fullyQualifiedMethodSymbols = customAssertionMethods.isEmpty() ? new String[0] : customAssertionMethods.split(",");
      List<MethodMatchers> customMethodMatchers = new ArrayList<>(fullyQualifiedMethodSymbols.length);
      for (String fullyQualifiedMethodSymbol : fullyQualifiedMethodSymbols) {
        String[] methodMatcherParts = fullyQualifiedMethodSymbol.split("#");
        if (methodMatcherParts.length == 2 && !isEmpty(methodMatcherParts[0].trim()) && !isEmpty(methodMatcherParts[1].trim())) {
          String methodName = methodMatcherParts[1].trim();
          Predicate<String> namePredicate;
          if (methodName.endsWith("*")) {
            namePredicate = name -> name.startsWith(methodName.substring(0, methodName.length() - 1));
          } else {
            namePredicate = name -> name.equals(methodName);
          }
          customMethodMatchers.add(MethodMatchers.create().ofSubTypes(methodMatcherParts[0].trim()).name(namePredicate).withAnyParameters().build());
        } else {
          LOG.warn("Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: '{}'", fullyQualifiedMethodSymbol);
        }
      }

      customAssertionMethodsMatcher = MethodMatchers.or(customMethodMatchers);
    }

    return customAssertionMethodsMatcher;
  }

  private static boolean expectAssertion(MethodTree methodTree) {
    List<SymbolMetadata.AnnotationValue> annotationValues = methodTree.symbol().metadata().valuesForAnnotation("org.junit.Test");
    if (annotationValues != null) {
      for (SymbolMetadata.AnnotationValue annotationValue : annotationValues) {
        if ("expected".equals(annotationValue.name())) {
          return true;
        }
      }
    }
    return false;
  }

  private class AssertionVisitor extends AbstractAssertionVisitor {
    private MethodMatchers customMethodsMatcher;

    private AssertionVisitor(MethodMatchers customMethodsMatcher) {
      this.customMethodsMatcher = customMethodsMatcher;
    }

    @Override
    protected boolean isAssertion(Symbol methodSymbol) {
      return customMethodsMatcher.matches(methodSymbol) || isLocalMethodWithAssertion(methodSymbol);
    }
  }

}
