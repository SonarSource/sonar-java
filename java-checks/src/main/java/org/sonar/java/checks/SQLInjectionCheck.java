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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2077",
  name = "Values passed to SQL commands should be sanitized",
  priority = Priority.CRITICAL,
  tags = {Tag.CWE, Tag.OWASP_A1, Tag.SANS_TOP_25_INSECURE, Tag.SECURITY, Tag.SQL, Tag.HIBERNATE})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INPUT_VALIDATION_AND_REPRESENTATION)
@SqaleConstantRemediation("20min")
public class SQLInjectionCheck extends AbstractInjectionChecker {

  private static final MethodMatcher HIBERNATE_SESSION_CREATE_QUERY_MATCHER = MethodMatcher.create()
    // method from the interface org.hibernate.SharedSessionContract, implemented by org.hibernate.Session
    .callSite(TypeCriteria.subtypeOf("org.hibernate.Session"))
    .name("createQuery")
    .withNoParameterConstraint();

  private static final MethodMatcher STATEMENT_EXECUTE_QUERY_MATCHER = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("java.sql.Statement"))
    .name("executeQuery")
    .withNoParameterConstraint();

  private static final MethodInvocationMatcherCollection CONNECTION_MATCHERS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.sql.Connection")).name("prepareStatement").withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.sql.Connection")).name("prepareCall").withNoParameterConstraint());

  private static final MethodMatcher ENTITY_MANAGER_CREATE_NATIVE_QUERY_MATCHER = MethodMatcher.create()
    .typeDefinition("javax.persistence.EntityManager")
    .name("createNativeQuery")
    .withNoParameterConstraint();

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodTree = (MethodInvocationTree) tree;
    boolean isHibernateCall = isHibernateCall(methodTree);
    if (isHibernateCall || isExecuteQueryOrPrepareStatement(methodTree) || isEntityManagerCreateNativeQuery(methodTree)) {
      //We want to check the argument for the three methods.
      ExpressionTree arg = methodTree.arguments().get(0);
      parameterName = "";
      if (isDynamicString(methodTree, arg, null, true)) {
        String message = "\"" + parameterName + "\" is provided externally to the method and not sanitized before use.";
        if (isHibernateCall) {
          message = "Use Hibernate's parameter binding instead of concatenation.";
        }
        addIssue(methodTree, message);
      }
    }
  }

  private static boolean isExecuteQueryOrPrepareStatement(MethodInvocationTree methodTree) {
    return !methodTree.arguments().isEmpty() && (STATEMENT_EXECUTE_QUERY_MATCHER.matches(methodTree) || CONNECTION_MATCHERS.anyMatch(methodTree));
  }

  private static boolean isHibernateCall(MethodInvocationTree methodTree) {
    return HIBERNATE_SESSION_CREATE_QUERY_MATCHER.matches(methodTree);
  }

  private static boolean isEntityManagerCreateNativeQuery(MethodInvocationTree methodTree) {
    return ENTITY_MANAGER_CREATE_NATIVE_QUERY_MATCHER.matches(methodTree);
  }
}
