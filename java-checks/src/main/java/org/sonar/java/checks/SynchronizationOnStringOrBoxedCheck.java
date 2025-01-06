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
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1860")
public class SynchronizationOnStringOrBoxedCheck extends IssuableSubscriptionVisitor {
  
  private static final MethodMatchers FORBIDDEN_MATCHERS = MethodMatchers.create()
    .ofTypes("java.util.List", "java.util.Map", "java.util.Set")
    .names("of", "copyOf", "ofEntries", "entry")
    .withAnyParameters()
    .build();
  
  private static final List<String> FORBIDDEN_SUBTYPES = Collections.singletonList("java.lang.ProcessHandle");
  
  private static final Set<String> FORBIDDEN_TYPES = SetUtils.immutableSetOf(
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Double",
    "java.lang.Float",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Short",
    "java.lang.String",
    "java.lang.Runtime.Version",
    "java.util.Optional",
    "java.util.OptionalInt",
    "java.util.OptionalLong",
    "java.util.OptionalDouble",
    "java.time.Instant",
    "java.time.LocalDate",
    "java.time.LocalTime",
    "java.time.LocalDateTime",
    "java.time.ZonedDateTime",
    "java.time.ZoneId",
    "java.time.OffsetTime",
    "java.time.OffsetDateTime",
    "java.time.ZoneOffset",
    "java.time.Duration",
    "java.time.Period",
    "java.time.Year",
    "java.time.YearMonth",
    "java.time.MonthDay",
    "java.time.chrono.MinguoDate",
    "java.time.chrono.HijrahDate",
    "java.time.chrono.ThaiBuddhistDate"
    );

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SynchronizedStatementTree syncStatement = (SynchronizedStatementTree) tree;
    ExpressionTree expression = syncStatement.expression();
    Type expressionType = expression.symbolType();
    if (expressionType.isPrimitive() || isForbiddenType(expressionType) || isInitializedWithImmutable(expression)) {
      reportIssue(expression, "Synchronize on a new \"Object\" instead.");
    }
  }

  private static boolean isForbiddenType(Type expressionType) {
    return FORBIDDEN_TYPES.contains(expressionType.fullyQualifiedName()) 
      || FORBIDDEN_SUBTYPES.stream().anyMatch(expressionType::isSubtypeOf);
  }
  
  private static boolean isInitializedWithImmutable(ExpressionTree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      return FORBIDDEN_MATCHERS.matches(((MethodInvocationTree) tree));
    } else if (tree.is(Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) tree;
      return Optional.ofNullable(identifierTree.symbol().declaration())
        .filter(decl -> decl.is(Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .flatMap(variable -> Optional.ofNullable(variable.initializer()))
        .filter(init -> init.is(Kind.METHOD_INVOCATION) && FORBIDDEN_MATCHERS.matches(((MethodInvocationTree) init)))
        .isPresent();
    }
    return false;
  }

}
