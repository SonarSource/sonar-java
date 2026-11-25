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

import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.PatternTree;

/** Lambdas, method references and patterns, not translated yet: a file using them is parsed by the default frontend. */
final class LambdaTranslator extends TranslatorPart {

  LambdaTranslator(TranslationState state) {
    super(state);
  }

  void bindLambdaParameterOwner(LambdaExpressionTreeImpl lambda, Type target) {
    throw new UnsupportedConstructException("lambda");
  }

  Type referenceResult(MethodReferenceTreeImpl reference, Type function) {
    throw new UnsupportedConstructException("method reference");
  }

  PatternTree pattern(CstNode node) {
    throw new UnsupportedConstructException(node.k());
  }

  LambdaExpressionTreeImpl lambda(CstNode node) {
    throw new UnsupportedConstructException(node.k());
  }

  MethodReferenceTreeImpl methodReference(CstNode node) {
    throw new UnsupportedConstructException(node.k());
  }
}
