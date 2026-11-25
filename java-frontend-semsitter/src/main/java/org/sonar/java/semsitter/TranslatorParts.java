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

/** The parts that translate one document, each created once and sharing one {@link TranslationState}. */
final class TranslatorParts {
  final DeclarationTranslator declarations;
  final MemberTranslator members;
  final StatementTranslator statements;
  final SwitchTranslator switches;
  final NameTranslator names;
  final ExpressionTranslator expressions;
  final InvocationTranslator invocations;
  final LambdaTranslator lambdas;
  final TypeTranslator typeTrees;

  TranslatorParts(TranslationState state) {
    declarations = new DeclarationTranslator(state);
    members = new MemberTranslator(state);
    statements = new StatementTranslator(state);
    switches = new SwitchTranslator(state);
    names = new NameTranslator(state);
    expressions = new ExpressionTranslator(state);
    invocations = new InvocationTranslator(state);
    lambdas = new LambdaTranslator(state);
    typeTrees = new TypeTranslator(state);
  }
}
