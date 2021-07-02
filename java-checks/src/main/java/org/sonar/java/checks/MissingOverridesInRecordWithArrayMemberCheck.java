package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6218")
public class MissingOverridesInRecordWithArrayMemberCheck extends IssuableSubscriptionVisitor {
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
    return Arrays.asList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree targetRecord = (ClassTree) tree;

    boolean recordHasArrayComponent = targetRecord.recordComponents().stream()
      .anyMatch(component -> component.symbol().type().isArray());
    if (!recordHasArrayComponent) {
      return;
    }

    Optional<String> message = inspectRecord(targetRecord);
    if (message.isPresent()) {
      reportIssue(targetRecord, message.get());
    }
  }

  public static Optional<String> inspectRecord(ClassTree tree) {
    boolean equals = false;
    boolean hashCode = false;
    boolean toString = false;
    for (Tree member : tree.members()) {
      if (!member.is(Tree.Kind.METHOD)) {
        continue;
      }
      MethodTree method = (MethodTree) member;
      if (EQUALS_MATCHER.matches(method)) {
        equals = true;
      } else if (HASH_CODE_MATCHER.matches(method)) {
        hashCode = true;
      } else if (TO_STRING_MATCHER.matches(method)) {
        toString = true;
      }
    }
    return Optional.ofNullable(computeMessage(equals, hashCode, toString));
  }

  private static String computeMessage(boolean equalsIsOverridden, boolean hashCodeIsOverridden, boolean toStringIsOverridden) {
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

    if (missingOverrides.isEmpty()) {
      return null;
    }
    if (missingOverrides.size() == 1) {
      return String.format(MESSAGE_TEMPLATE, missingOverrides.get(0));
    }
    StringBuilder sequence = new StringBuilder(missingOverrides.get(0));
    if (missingOverrides.size() == 3) {
      sequence.append(", " + missingOverrides.get(1));
    }
    sequence.append(" and " + missingOverrides.get(missingOverrides.size() - 1));
    return String.format(MESSAGE_TEMPLATE, sequence);
  }
}
