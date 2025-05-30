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

import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2885")
public class StaticMultithreadedUnsafeFieldsCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_TEXT_SIMPLE_DATE_FORMAT = "java.text.SimpleDateFormat";
  private static final String[] FORBIDDEN_TYPES = {JAVA_TEXT_SIMPLE_DATE_FORMAT, "java.util.Calendar", "javax.xml.xpath.XPath", "javax.xml.validation.SchemaFactory"};
  private static final MethodMatchers GET_DATE_INSTANCE = MethodMatchers.create()
    .ofTypes("java.text.DateFormat")
    .names("getDateInstance")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    Type type = variableTree.type().symbolType();
    if (ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC) && isForbidden(variableTree)) {
      if (type.isSubtypeOf(JAVA_TEXT_SIMPLE_DATE_FORMAT) && onlySynchronizedUsages((Symbol.VariableSymbol) variableTree.symbol())) {
        return;
      }
      IdentifierTree identifierTree = variableTree.simpleName();
      reportIssue(identifierTree, String.format("Make \"%s\" an instance variable.", identifierTree.name()));
    }
  }

  private static boolean isForbidden(VariableTree variableTree) {
    if (isForbiddenType(variableTree.type().symbolType())) {
      return true;
    }
    ExpressionTree initializer = variableTree.initializer();
    if (initializer == null || initializer.is(Tree.Kind.NULL_LITERAL)) {
      return false;
    }
    return isForbiddenType(initializer.symbolType())
      || (initializer.is(Tree.Kind.METHOD_INVOCATION) && GET_DATE_INSTANCE.matches((MethodInvocationTree) initializer));
  }

  private static boolean isForbiddenType(Type type) {
    for (String name : FORBIDDEN_TYPES) {
      if (type.isSubtypeOf(name)) {
        return true;
      }
    }
    return false;
  }

  private static boolean onlySynchronizedUsages(Symbol.VariableSymbol variable) {
    List<IdentifierTree> usages = variable.usages();
    if (usages.isEmpty()) {
      return false;
    }
    for (IdentifierTree usage : usages) {
      SynchronizedStatementTree synchronizedStatementTree = getParentSynchronizedStatement(usage);
      if (synchronizedStatementTree == null) {
        // used outside a synchronized statement
        return false;
      } else {
        ExpressionTree expression = synchronizedStatementTree.expression();
        if (!expression.is(Tree.Kind.IDENTIFIER) || !variable.equals(((IdentifierTree) expression).symbol())) {
          // variable is not the expression synchronized
          return false;
        }
        // check other usages
      }
    }
    return true;
  }

  @CheckForNull
  private static SynchronizedStatementTree getParentSynchronizedStatement(IdentifierTree usage) {
    Tree parent = usage.parent();
    while (parent != null && !parent.is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
      parent = parent.parent();
    }
    if (parent == null) {
      return null;
    }
    return (SynchronizedStatementTree) parent;
  }

}
