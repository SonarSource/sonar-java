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
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "RightCurlyBraceSameLineAsNextBlockCheck",
  name = "Close curly brace and the next \"else\", \"catch\" and \"finally\" keywords should be located on the same line",
  priority = Priority.MINOR,
  tags = {Tag.CONVENTION})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class RightCurlyBraceSameLineAsNextBlockCheck extends RightCurlyBraceToNextBlockAbstractVisitor {

  @Override
  protected void checkTokenPosition(SyntaxToken syntaxToken, BlockTree previousBlock) {
    if (syntaxToken.line() != previousBlock.closeBraceToken().line()) {
      reportIssue(syntaxToken, "Move this \"" + syntaxToken.text() + "\" on the same line that the previous closing curly brace.");
    }
  }
}
