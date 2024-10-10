/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

import static org.sonar.java.model.ModifiersUtils.hasModifier;

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
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    Type type = variableTree.type().symbolType();
    if (hasModifier(variableTree.modifiers(), Modifier.STATIC) &&
      isForbidden(variableTree) &&
      (!type.isSubtypeOf(JAVA_TEXT_SIMPLE_DATE_FORMAT) ||
        hasEmptyOrNonSynchronizedUsages((Symbol.VariableSymbol) variableTree.symbol()))) {
      IdentifierTree identifierTree = variableTree.simpleName();
      reportIssue(identifierTree, String.format("Make \"%s\" an instance variable.", identifierTree.name()));
    }
  }

  private static boolean isForbidden(VariableTree variableTree) {
    if (isForbiddenType(variableTree.type().symbolType())) {
      return true;
    }
    ExpressionTree initializer = variableTree.initializer();
    if (initializer == null || initializer.is(Kind.NULL_LITERAL)) {
      return false;
    }
    return isForbiddenType(initializer.symbolType())
      || (initializer.is(Kind.METHOD_INVOCATION) && GET_DATE_INSTANCE.matches((MethodInvocationTree) initializer));
  }

  private static boolean isForbiddenType(Type type) {
    for (String name : FORBIDDEN_TYPES) {
      if (type.isSubtypeOf(name)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasEmptyOrNonSynchronizedUsages(Symbol.VariableSymbol variable) {
    List<IdentifierTree> usages = variable.usages();
    if (usages.isEmpty()) {
      return true;
    }
    for (IdentifierTree usage : usages) {
      if (isNonSynchronizedUsage(usage, variable)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNonSynchronizedUsage(IdentifierTree usage, Symbol.VariableSymbol variable) {
    Tree parent = usage.parent();
    while (parent != null && !parent.is(Kind.LAMBDA_EXPRESSION, Kind.CONSTRUCTOR, Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.RECORD, Kind.ANNOTATION_TYPE)) {
      if (parent.is(Kind.SYNCHRONIZED_STATEMENT)) {
        ExpressionTree expression = ((SynchronizedStatementTree) parent).expression();
        if (expression.is(Kind.IDENTIFIER) && variable.equals(((IdentifierTree) expression).symbol())) {
          return false;
        }
      } else if (parent.is(Kind.METHOD)) {
        ModifiersTree modifiers = ((MethodTree) parent).modifiers();
        return !hasModifier(modifiers, Modifier.STATIC) || !hasModifier(modifiers, Modifier.SYNCHRONIZED);
      }
      parent = parent.parent();
    }
    return true;
  }

}
