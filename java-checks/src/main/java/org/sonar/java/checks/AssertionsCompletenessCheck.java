/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Objects;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2970",
  name = "Assertions should be complete",
  priority = Priority.CRITICAL,
  tags = {Tag.BUG, Tag.TESTS})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class AssertionsCompletenessCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String FEST_ASSERT_SUPERTYPE = "org.fest.assertions.Assert";
  private static final String ASSERTJ_SUPERTYPE = "org.assertj.core.api.AbstractAssert";
  private static final MethodMatcher MOCKITO_VERIFY = MethodMatcher.create()
    .typeDefinition("org.mockito.Mockito").name("verify").withNoParameterConstraint();

  private static final MethodMatcherCollection FEST_LIKE_ASSERT_THAT = MethodMatcherCollection.create(
    // Fest 1.X
    assertThatOnType("org.fest.assertions.Assertions"),
    // Fest 2.X
    assertThatOnType("org.fest.assertions.api.Assertions"),
    // AssertJ 1.X
    assertThatOnType("org.assertj.core.api.AbstractSoftAssertions"),
    // AssertJ 2.X
    assertThatOnType("org.assertj.core.api.Assertions"),
    assertThatOnType("org.assertj.core.api.AbstractStandardSoftAssertions"),
    // AssertJ 3.X
    assertThatOnType("org.assertj.core.api.StrictAssertions")
  );

  private static MethodMatcher assertThatOnType(String type) {
    return MethodMatcher.create().typeDefinition(type).name("assertThat").addParameter(TypeCriteria.anyType());
  }
  private static final MethodMatcherCollection FEST_LIKE_EXCLUSIONS = MethodMatcherCollection.create(
    methodWithName(FEST_ASSERT_SUPERTYPE, NameCriteria.startsWith("as")),
    methodWithName(FEST_ASSERT_SUPERTYPE, NameCriteria.startsWith("using")),
    methodWithName(FEST_ASSERT_SUPERTYPE, NameCriteria.startsWith("with")),
    methodWithName(FEST_ASSERT_SUPERTYPE, NameCriteria.is("describedAs")),
    methodWithName(FEST_ASSERT_SUPERTYPE, NameCriteria.is("overridingErrorMessage")),
    methodWithName(ASSERTJ_SUPERTYPE, NameCriteria.startsWith("as")),
    methodWithName(ASSERTJ_SUPERTYPE, NameCriteria.startsWith("using")),
    methodWithName(ASSERTJ_SUPERTYPE, NameCriteria.startsWith("with")),
    methodWithName(ASSERTJ_SUPERTYPE, NameCriteria.is("describedAs")),
    methodWithName(ASSERTJ_SUPERTYPE, NameCriteria.is("overridingErrorMessage"))
  );

  private static MethodMatcher methodWithName(String superType, NameCriteria nameCriteria) {
    return MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(superType)).name(nameCriteria).withNoParameterConstraint();
  }

  private Boolean chainedToAnyMethodButFestExclusions = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skip variable assignments
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    // skip return statements
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    if (incompleteAssertion(mit)) {
      return;
    }
    Boolean previous = chainedToAnyMethodButFestExclusions;
    chainedToAnyMethodButFestExclusions = Objects.firstNonNull(chainedToAnyMethodButFestExclusions, false) || !FEST_LIKE_EXCLUSIONS.anyMatch(mit);
    scan(mit.methodSelect());
    // skip arguments
    chainedToAnyMethodButFestExclusions = previous;
  }

  private boolean incompleteAssertion(MethodInvocationTree mit) {
    if (((FEST_LIKE_ASSERT_THAT.anyMatch(mit) && (mit.arguments().size() == 1)) || MOCKITO_VERIFY.matches(mit)) && !Boolean.TRUE.equals(chainedToAnyMethodButFestExclusions)) {
      context.reportIssue(this, mit.methodSelect(), "Complete the assertion.");
      return true;
    }
    return false;
  }

}
