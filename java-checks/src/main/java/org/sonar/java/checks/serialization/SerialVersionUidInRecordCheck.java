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
package org.sonar.java.checks.serialization;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6219")
public class SerialVersionUidInRecordCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree targetRecord = (ClassTree) tree;
    if (!targetRecord.symbol().type().isSubtypeOf("java.io.Serializable")) {
      return;
    }
    for (Tree member : targetRecord.members()) {
      if (!member.is(Tree.Kind.VARIABLE)) {
        continue;
      }
      VariableTree variable = (VariableTree) member;
      if (isSerialVersionUIDField(variable) && setsTheValueToZero(variable)) {
        reportIssue(variable, "Remove this redundant \"serialVersionUID\" field");
        return;
      }
    }
  }

  private static boolean isSerialVersionUIDField(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return symbol.isFinal() &&
      symbol.type().is("long") &&
      "serialVersionUID".equals(symbol.name());
  }

  private static boolean setsTheValueToZero(VariableTree variable) {
    ExpressionTree initializer = variable.initializer();
    if (initializer == null) {
      return false;
    }
    Optional<Long> longValue = initializer.asConstant(Long.class);
    if (longValue.isPresent()) {
      return longValue.get() == 0L;
    }
    Optional<Integer> integerValue = initializer.asConstant(Integer.class);
    return integerValue.isPresent() && integerValue.get() == 0;
  }
}
