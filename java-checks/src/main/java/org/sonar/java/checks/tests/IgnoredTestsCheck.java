/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1607")
public class IgnoredTestsCheck extends IssuableSubscriptionVisitor {

  private static final String ORG_JUNIT_ASSUME = "org.junit.Assume";
  private static final String BOOLEAN_TYPE = "boolean";

  private static final MethodMatchers ASSUME_METHODS = MethodMatchers.create()
    .ofTypes(ORG_JUNIT_ASSUME).names("assumeTrue", "assumeFalse").addParametersMatcher(BOOLEAN_TYPE).build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    SymbolMetadata symbolMetadata = methodTree.symbol().metadata();
    // check for @Ignore or @Disabled annotations
    boolean hasIgnoreAnnotation = isSilentlyIgnored(symbolMetadata, "org.junit.Ignore");
    boolean hasDisabledAnnotation = isSilentlyIgnored(symbolMetadata, "org.junit.jupiter.api.Disabled");
    if (hasIgnoreAnnotation || hasDisabledAnnotation) {
      reportIssue(methodTree.simpleName(), String.format("Either add an explanation about why this test is skipped or remove the " +
        "\"@%s\" annotation.", hasIgnoreAnnotation ? "Ignore" : "Disabled"));
    }

    // check for "assumeFalse(true)" and "assumeTrue(false)"-calls, which may also result in permanent skipping of the given test
    BlockTree block = methodTree.block();
    if (block != null) {
      block.body().stream()
        .filter(s -> s.is(Tree.Kind.EXPRESSION_STATEMENT))
        .map(s -> ((ExpressionStatementTree) s).expression())
        .filter(s -> s.is(Tree.Kind.METHOD_INVOCATION))
        .map(MethodInvocationTree.class::cast)
        .filter(ASSUME_METHODS::matches)
        .filter(IgnoredTestsCheck::hasConstantOppositeArg)
        .forEach(mit -> {
          List<JavaFileScannerContext.Location> secondaryLocation = Collections.singletonList(new JavaFileScannerContext.Location(
            "A constant boolean value is passed as argument, causing this test to always be skipped.", mit.arguments()));

          reportIssue(ExpressionUtils.methodName(mit), "This assumption is called with a boolean constant; remove it or, to skip this " +
              "test use an @Ignore/@Disabled annotation in combination with an explanation about why it is skipped.",
            secondaryLocation, null);
        });
    }
  }

  private static boolean isSilentlyIgnored(SymbolMetadata symbolMetadata, String fullyQualifiedName) {
    // This code duplicates the behavior of SymbolMetadata.valuesForAnnotation but checks for broken semantics
    for (SymbolMetadata.AnnotationInstance annotation : symbolMetadata.annotations()) {
      Type type = annotation.symbol().type();
      // In case of broken semantics, the annotation may match the fully qualified name but still miss the type binding.
      // As a consequence, fetching the values from the annotation returns an empty list, as if there were no value, even though there might be one or more.
      // In such cases, it is best to consider that the test is not ignored.
      if (type.isUnknown()) {
        return false;
      }
      if (type.is(fullyQualifiedName)) {
        return annotation.values().isEmpty();
      }
    }
    return false;
  }

  private static boolean hasConstantOppositeArg(MethodInvocationTree mit) {
    Optional<Boolean> result = mit.arguments().get(0).asConstant(Boolean.class);
    return result.isPresent() && !result.get().equals(mit.methodSymbol().name().contains("True"));
  }
}
