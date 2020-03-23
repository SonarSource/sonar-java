/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.java.checks.helpers.UnitTestUtils.hasJUnit5TestAnnotation;
import static org.sonar.java.model.ExpressionUtils.methodName;

@Rule(key = "S2699")
public class AssertionsInTestsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Logger LOG = Loggers.get(AssertionsInTestsCheck.class);

  private static final Pattern ASSERTION_METHODS_PATTERN = Pattern.compile("(assert|verify|fail|should|check|expect|validate).*");
  private static final Pattern TEST_METHODS_PATTERN = Pattern.compile("test.*|.*Test");

  private static final MethodMatchers ASSERTION_INVOCATION_MATCHERS = MethodMatchers.or(
    // fest 1.x / 2.X
    MethodMatchers.create().ofSubTypes("org.fest.assertions.GenericAssert", "org.fest.assertions.api.AbstractAssert").anyName().withAnyParameters().build(),
    // rest assured 2.0
    MethodMatchers.create().ofTypes("io.restassured.response.ValidatableResponseOptions")
      .name(name -> name.equals("body") ||
        name.equals("time") ||
        name.startsWith("time") ||
        name.startsWith("content") ||
        name.startsWith("status") ||
        name.startsWith("header") ||
        name.startsWith("cookie") ||
        name.startsWith("spec"))
      .withAnyParameters()
      .build(),
    // assertJ
    MethodMatchers.create().ofSubTypes("org.assertj.core.api.AbstractAssert").anyName().withAnyParameters().build(),
    // spring
    MethodMatchers.create().ofTypes("org.springframework.test.web.servlet.ResultActions").names("andExpect").addParametersMatcher(t -> true).build(),
    // JMockit
    MethodMatchers.create().ofTypes("mockit.Verifications").constructor().withAnyParameters().build(),
    // Eclipse Vert.x
    MethodMatchers.create().ofTypes("io.vertx.ext.unit.TestContext").name(name -> name.startsWith("asyncAssert")).addWithoutParametersMatcher().build());

  private static final MethodMatchers REACTIVE_X_TEST_METHODS =
    MethodMatchers.create().ofSubTypes("rx.Observable", "io.reactivex.Observable").names("test").withAnyParameters().build();

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

    if (isUnitTest(methodTree) && !expectAssertion(methodTree) && !isLocalMethodWithAssertion(methodTree.symbol())) {
      context.reportIssue(this, methodTree.simpleName(), "Add at least one assertion to this test case.");
    }
  }

  private boolean isLocalMethodWithAssertion(Symbol symbol) {
    if (!assertionInMethod.containsKey(symbol)) {
      assertionInMethod.put(symbol, false);
      Tree declaration = symbol.declaration();
      if (declaration != null) {
        AssertionVisitor assertionVisitor = new AssertionVisitor(getCustomAssertionMethodsMatcher());
        declaration.accept(assertionVisitor);
        assertionInMethod.put(symbol, assertionVisitor.hasAssertion);
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

  private static boolean isUnitTest(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    while (symbol != null) {
      if (symbol.metadata().isAnnotatedWith("org.junit.Test")) {
        return true;
      }
      symbol = symbol.overriddenSymbol();
    }

    if (hasJUnit5TestAnnotation(methodTree)) {
      // contrary to JUnit 4, JUnit 5 Test annotations are not inherited when method is overridden, so no need to check overridden symbols
      return true;
    }
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    return enclosingClass != null && enclosingClass.type().isSubtypeOf("junit.framework.TestCase") && methodTree.simpleName().name().startsWith("test");
  }

  private class AssertionVisitor extends BaseTreeVisitor {
    boolean hasAssertion = false;
    private MethodMatchers customMethodsMatcher;

    private AssertionVisitor(MethodMatchers customMethodsMatcher) {
      this.customMethodsMatcher = customMethodsMatcher;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      super.visitMethodInvocation(mit);
      if (!hasAssertion && isAssertion(methodName(mit), mit.symbol())) {
        hasAssertion = true;
      }
    }

    @Override
    public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
      super.visitMethodReference(methodReferenceTree);
      if (!hasAssertion && isAssertion(methodReferenceTree.method(), methodReferenceTree.method().symbol())) {
        hasAssertion = true;
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      super.visitNewClass(tree);
      if (!hasAssertion && isAssertion(null, tree.constructorSymbol())) {
        hasAssertion = true;
      }
    }

    private boolean isAssertion(@Nullable IdentifierTree method, Symbol methodSymbol) {
      return matchesMethodPattern(method, methodSymbol)
        || ASSERTION_INVOCATION_MATCHERS.matches(methodSymbol)
        || customMethodsMatcher.matches(methodSymbol)
        || isLocalMethodWithAssertion(methodSymbol);
    }

    private boolean matchesMethodPattern(@Nullable IdentifierTree method, Symbol methodSymbol) {
      if (method == null) {
        return false;
      }

      String methodName = method.name();
      if (TEST_METHODS_PATTERN.matcher(methodName).matches()) {
        return !REACTIVE_X_TEST_METHODS.matches(methodSymbol);
      }
      return ASSERTION_METHODS_PATTERN.matcher(methodName).matches();
    }
  }

}
