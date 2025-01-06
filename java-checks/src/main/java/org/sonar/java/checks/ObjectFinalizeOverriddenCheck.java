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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ObjectFinalizeOverridenCheck", repositoryKey = "squid")
@Rule(key = "S1113")
public class ObjectFinalizeOverriddenCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers FINALIZE_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.Object")
    .names("finalize")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (FINALIZE_MATCHER.matches(methodTree) && isNotFinalOrHasNonEmptyBody(methodTree)) {
      reportIssue(methodTree.simpleName(), "Do not override the Object.finalize() method.");
    }
  }

  private static boolean isNotFinalOrHasNonEmptyBody(MethodTree methodTree) {
    return !ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.FINAL)
      || !Objects.requireNonNull(methodTree.block()).body().isEmpty();
  }
}
