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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.MethodTreeUtils.subsequentMethodInvocation;
import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S5970")
public class SpringAssertionsSimplificationCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_TEMPLATE = "Replace this assertion by \"%s\".";

  private static final String[] ASSERTION_CLASSES = {
    // JUnit4
    "org.junit.Assert",
    "junit.framework.TestCase",
    // JUnit4 (deprecated)
    "junit.framework.Assert",
    // JUnit5
    "org.junit.jupiter.api.Assertions"
  };

  private static final String[] ASSERT_J_AND_FEST_ASSERT = {"org.assertj.core.api.AbstractAssert", "org.fest.assertions.GenericAssert"};

  private static final MethodMatchers ASSERT_EQUALS_MATCHER = MethodMatchers.create()
    .ofTypes(ASSERTION_CLASSES)
    .names("assertEquals")
    .addParametersMatcher(ANY, ANY)
    .build();

  private static final MethodMatchers ASSERT_TRUE_FALSE_EQUALS_MATCHER = MethodMatchers.create()
    .ofTypes(ASSERTION_CLASSES)
    .names("assertEquals", "assertTrue", "assertFalse")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MODEL_VIEW_GET_VIEW_NAME = MethodMatchers.create()
    .ofTypes("org.springframework.web.servlet.ModelAndView")
    .names("getViewName")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers MODEL_MAP_GET = MethodMatchers.create()
    .ofTypes("org.springframework.ui.ModelMap")
    .names("get")
    .addParametersMatcher("java.lang.Object")
    .build();

  private static final MethodMatchers ASSERT_THAT = MethodMatchers.create()
    .ofTypes("org.assertj.core.api.Assertions", "org.fest.assertions.Assertions")
    .names("assertThat")
    .withAnyParameters()
    .build();

  private static final MethodMatchers IS_EQUAL_TO = MethodMatchers.create()
    .ofSubTypes(ASSERT_J_AND_FEST_ASSERT)
    .names("isEqualTo")
    .withAnyParameters()
    .build();

  private static final MethodMatchers IS_EQUAL_TO_IS_FALSE_IS_TRUE = MethodMatchers.create()
    .ofSubTypes(ASSERT_J_AND_FEST_ASSERT)
    .names("isEqualTo", "isTrue", "isFalse")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (MODEL_VIEW_GET_VIEW_NAME.matches(mit)) {
      getNestingCall(tree).ifPresent(call -> {
          if (ASSERT_EQUALS_MATCHER.matches(call) ||
            (ASSERT_THAT.matches(call) && subsequentMethodInvocation(call, IS_EQUAL_TO).isPresent())) {
            reportIssue(ExpressionUtils.methodName(call), String.format(MESSAGE_TEMPLATE, "ModelAndViewAssert.assertViewName"));
          }
        }
      );
    } else if (MODEL_MAP_GET.matches(mit)) {
      getNestingCall(tree).ifPresent(call -> {
        if (ASSERT_TRUE_FALSE_EQUALS_MATCHER.matches(call) ||
          (ASSERT_THAT.matches(call) && subsequentMethodInvocation(call, IS_EQUAL_TO_IS_FALSE_IS_TRUE).isPresent())) {
          reportIssue(ExpressionUtils.methodName(call), String.format(MESSAGE_TEMPLATE, "ModelAndViewAssert.assertModelAttributeValue"));
        }
      });
    }
  }

  private static Optional<MethodInvocationTree> getNestingCall(Tree nestedTree) {
    Tree parent = nestedTree.parent();
    // This pattern should be used with care, since it is not efficient and can hit performance.
    // It is acceptable in this case though, since we will call this method only when facing Spring methods and
    // situation that will hardly ever occur compared to common assertions.
    while (parent != null && !parent.is(Tree.Kind.METHOD)) {
      if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
        return Optional.of((MethodInvocationTree) parent);
      }
      parent = parent.parent();
    }
    return Optional.empty();
  }

}
