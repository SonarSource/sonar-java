/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
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

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    Type type = variableTree.type().symbolType();
    if (ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC) && isForbiddenType(variableTree)) {
      if (type.isSubtypeOf(JAVA_TEXT_SIMPLE_DATE_FORMAT) && onlySynchronizedUsages((Symbol.VariableSymbol) variableTree.symbol())) {
        return;
      }
      IdentifierTree identifierTree = variableTree.simpleName();
      reportIssue(identifierTree, String.format("Make \"%s\" an instance variable.", identifierTree.name()));
    }
  }

  private static boolean isForbiddenType(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    return isForbiddenType(variableTree.type().symbolType()) || (initializer != null && !initializer.is(Tree.Kind.NULL_LITERAL) && isForbiddenType(initializer.symbolType()));
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
