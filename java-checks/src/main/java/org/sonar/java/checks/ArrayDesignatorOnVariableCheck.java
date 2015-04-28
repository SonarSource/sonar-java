/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.sonar.sslr.api.AstNode;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.TreeFactory;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1197",
  name = "Array designators \"[]\" should be on the type, not the variable",
  tags = {"convention"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class ArrayDesignatorOnVariableCheck extends SquidCheck<LexerlessGrammar> implements JavaCheck {

  @Override
  public void init() {
    subscribeTo(Kind.VARIABLE);
    subscribeTo(JavaLexer.VARIABLE_DECLARATOR);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.hasDirectChildren(TreeFactory.WRAPPER_AST_NODE) || (node.is(Kind.VARIABLE) && ((VariableTreeImpl) node).dims() > 0)) {
      getContext().createLineViolation(this, "Move the array designator from the variable to the type.", node);
    }
  }

}
