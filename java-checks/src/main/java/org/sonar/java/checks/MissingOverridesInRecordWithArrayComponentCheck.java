/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6218")
public class MissingOverridesInRecordWithArrayComponentCheck extends IssuableSubscriptionVisitor {
  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("equals")
    .addParametersMatcher("java.lang.Object")
    .build();

  private static final MethodMatchers HASH_CODE_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("hashCode")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers TO_STRING_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("toString")
    .addWithoutParametersMatcher()
    .build();

  private static final String MESSAGE_TEMPLATE = "Override %s to consider array's content in the method";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree targetRecord = (ClassTree) tree;

    List<VariableTree> recordArrayComponents = targetRecord.recordComponents().stream()
      .filter(component -> component.symbol().type().isArray())
      .toList();

    if (recordArrayComponents.isEmpty()) {
      return;
    }

    inspectRecord(targetRecord)
      .ifPresent(composedMessage -> reportIssue(targetRecord.simpleName(), composedMessage, secondaries(recordArrayComponents), null));
  }

  private static List<JavaFileScannerContext.Location> secondaries(List<VariableTree> recordArrayComponents) {
    return recordArrayComponents.stream()
      .map(arrayComponent -> new JavaFileScannerContext.Location("Array", arrayComponent))
      .toList();
  }

  public static Optional<String> inspectRecord(ClassTree tree) {
    boolean equalsIsOverridden = false;
    boolean hashCodeIsOverridden = false;
    boolean toStringIsOverridden = false;
    for (Tree member : tree.members()) {
      if (!member.is(Tree.Kind.METHOD)) {
        continue;
      }
      MethodTree method = (MethodTree) member;
      if (EQUALS_MATCHER.matches(method)) {
        equalsIsOverridden = true;
      } else if (HASH_CODE_MATCHER.matches(method)) {
        hashCodeIsOverridden = true;
      } else if (TO_STRING_MATCHER.matches(method)) {
        toStringIsOverridden = true;
      }
    }
    return computeMessage(equalsIsOverridden, hashCodeIsOverridden, toStringIsOverridden);
  }

  private static Optional<String> computeMessage(boolean equalsIsOverridden, boolean hashCodeIsOverridden, boolean toStringIsOverridden) {
    List<String> missingOverrides = new ArrayList<>(3);
    if (!equalsIsOverridden) {
      missingOverrides.add("equals");
    }
    if (!hashCodeIsOverridden) {
      missingOverrides.add("hashCode");
    }
    if (!toStringIsOverridden) {
      missingOverrides.add("toString");
    }

    String filler = null;
    switch (missingOverrides.size()) {
      case 0:
        return Optional.empty();
      case 1:
        filler = missingOverrides.get(0);
        break;
      case 2:
        filler = missingOverrides.get(0) + " and " + missingOverrides.get(1);
        break;
      default:
        filler = missingOverrides.get(0) + ", " + missingOverrides.get(1) + " and " + missingOverrides.get(2);
    }
    return Optional.of(String.format(MESSAGE_TEMPLATE, filler));
  }
}
