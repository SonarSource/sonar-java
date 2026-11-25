/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;

/** Switches and try statements, not translated yet: a file using them is parsed by the default frontend. */
final class SwitchTranslator extends TranslatorPart {

  SwitchTranslator(TranslationState state) {
    super(state);
  }

  TryStatementTreeImpl tryStatement(CstNode node) {
    throw new UnsupportedConstructException(node.k());
  }

  SwitchStatementTreeImpl switchStatement(CstNode node) {
    throw new UnsupportedConstructException(node.k());
  }

  SwitchExpressionTreeImpl switchExpression(CstNode node) {
    throw new UnsupportedConstructException(node.k());
  }
}
