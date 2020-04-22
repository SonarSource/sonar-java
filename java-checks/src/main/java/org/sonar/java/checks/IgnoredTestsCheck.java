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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
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
    if (!hasSemantic()) {
      return;
    }
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
        .forEach(mit -> reportIssue(mit, "Either remove this assumption or use an @Ignore or @Disabled annotation in combination with " +
          "an explanation about why this test is skipped."));
    }
  }

  private static boolean isSilentlyIgnored(SymbolMetadata symbolMetadata, String annotation) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbolMetadata.valuesForAnnotation(annotation);

    // check that an annotation of the given type is present and whether that annotation has any values that are passed to it (i.e. an
    // explanation for why the test is ignored)
    return annotationValues != null && annotationValues.isEmpty();
  }

  private static boolean hasConstantOppositeArg(MethodInvocationTree mit) {
    Optional<Boolean> result = mit.arguments().get(0).asConstant(Boolean.class);
    return result.isPresent() && !result.get().equals(mit.symbol().name().contains("True"));
  }
}
