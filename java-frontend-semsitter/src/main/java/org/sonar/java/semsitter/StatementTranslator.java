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

import java.util.List;
import org.sonar.java.model.statement.BlockTreeImpl;

import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.required;

/** Translates empty blocks. Statements are not translated yet: a file using them is parsed by the default frontend. */
final class StatementTranslator extends TranslatorPart {

  StatementTranslator(TranslationState state) {
    super(state);
  }

  BlockTreeImpl block(CstNode node) {
    if (!"block".equals(node.k())) {
      CstNode nested = childKind(node, "block");
      if (nested != null) {
        node = nested;
      }
    }
    for (CstNode child : node.children()) {
      if (!"{".equals(child.text()) && !"}".equals(child.text())) {
        throw new UnsupportedConstructException(child.k());
      }
    }
    return new BlockTreeImpl(token(required(node, "{")), List.of(), token(required(node, "}")));
  }
}
