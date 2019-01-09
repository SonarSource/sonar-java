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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Map;

@Rule(key = "S1488")
public class ImmediatelyReturnedVariableCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<Kind, String> MESSAGE_KEYS = ImmutableMap.of(
    Kind.THROW_STATEMENT, "throw",
    Kind.RETURN_STATEMENT, "return"
  );

  private JavaFileScannerContext context;
  private String lastTypeForMessage;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBlock(BlockTree tree) {
    super.visitBlock(tree);
    List<StatementTree> statements = tree.body();
    int size = statements.size();
    if (size < 2) {
      return;
    }
    StatementTree butLastStatement = statements.get(size - 2);
    if (butLastStatement.is(Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) butLastStatement;
      if(!variableTree.modifiers().annotations().isEmpty()) {
        return;
      }
      StatementTree lastStatement = statements.get(size - 1);
      String lastStatementIdentifier = getReturnOrThrowIdentifier(lastStatement);
      if (lastStatementIdentifier != null) {
        String identifier = variableTree.simpleName().name();
        if (StringUtils.equals(lastStatementIdentifier, identifier)) {
          context.reportIssue(
            this, variableTree.initializer(), "Immediately " + lastTypeForMessage + " this expression instead of assigning it to the temporary variable \"" + identifier + "\".");
        }
      }
    }

  }

  @CheckForNull
  private String getReturnOrThrowIdentifier(StatementTree lastStatementOfBlock) {
    lastTypeForMessage = null;
    ExpressionTree expr = null;
    if (lastStatementOfBlock.is(Kind.THROW_STATEMENT)) {
      lastTypeForMessage = MESSAGE_KEYS.get(Kind.THROW_STATEMENT);
      expr = ((ThrowStatementTree) lastStatementOfBlock).expression();
    } else if (lastStatementOfBlock.is(Kind.RETURN_STATEMENT)) {
      lastTypeForMessage = MESSAGE_KEYS.get(Kind.RETURN_STATEMENT);
      expr = ((ReturnStatementTree) lastStatementOfBlock).expression();
    }
    if (expr != null && expr.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) expr).name();
    }
    return null;
  }
}
