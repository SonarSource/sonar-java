/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
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
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Map;

@Rule(
  key = "S1488",
  name = "Local Variables should not be declared and then immediately returned or thrown",
  tags = {"clumsy"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class ImmediatelyReturnedVariableCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<Kind, String> MESSAGE_KEYS = ImmutableMap.of(Kind.THROW_STATEMENT, "throw", Kind.RETURN_STATEMENT, "return");

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
    if (statements.size() > 1) {
      StatementTree lastSatement = statements.get(statements.size() - 1);
      String lastStatementIdentifier = getReturnOrThrowIdentifier(lastSatement);
      if (StringUtils.isNotEmpty(lastStatementIdentifier)) {

        StatementTree butLastSatement = statements.get(statements.size() - 2);
        String identifier = getVariableDeclarationIdentifier(butLastSatement);

        if (StringUtils.equals(lastStatementIdentifier, identifier)) {
          context.addIssue(butLastSatement, this, "Immediately " + lastTypeForMessage + " this expression instead of assigning it to the temporary variable \"" + identifier
            + "\".");
        }
      }
    }

  }

  private String getReturnOrThrowIdentifier(StatementTree lastSatementOfBlock) {
    String result = null;
    lastTypeForMessage = null;
    ExpressionTree expr = null;
    if (lastSatementOfBlock.is(Kind.THROW_STATEMENT)) {
      lastTypeForMessage = MESSAGE_KEYS.get(Kind.THROW_STATEMENT);
      expr = ((ThrowStatementTree) lastSatementOfBlock).expression();
    } else if (lastSatementOfBlock.is(Kind.RETURN_STATEMENT)) {
      lastTypeForMessage = MESSAGE_KEYS.get(Kind.RETURN_STATEMENT);
      expr = ((ReturnStatementTree) lastSatementOfBlock).expression();
    }
    if (expr != null && expr.is(Kind.IDENTIFIER)) {
      result = ((IdentifierTree) expr).name();
    }
    return result;
  }

  private static String getVariableDeclarationIdentifier(StatementTree butLastSatement) {
    String result = null;
    if (butLastSatement.is(Kind.VARIABLE)) {
      result = ((VariableTree) butLastSatement).simpleName().name();
    }
    return result;
  }
}
