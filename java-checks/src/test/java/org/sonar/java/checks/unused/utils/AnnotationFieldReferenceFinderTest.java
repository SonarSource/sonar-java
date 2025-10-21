/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.unused.utils;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests an edge case of {@link AnnotationFieldReferenceFinder} that can not be covered with regular Java files by the tests in
 * {@link org.sonar.java.checks.unused.UnusedPrivateFieldCheckTest}.
 */
class AnnotationFieldReferenceFinderTest {
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.ERROR);

  @Test
  void should_graciously_handle_fields_with_overlapping_names() {
    var conflictingUsedA = field("conflictingUsed");
    var conflictingUsedB = field("conflictingUsed");
    var conflictingUnusedA = field("conflictingUnused");
    var conflictingUnusedB = field("conflictingUnused");
    var nonConflictingUsed = field("nonConflictingUsed");
    var nonConflictingUnused = field("nonConflictingUnused");

    var method = methodWithAnnotationArgs(
      literal("conflictingUsed"),
      literal("nonConflictingUsed"));

    var finder = AnnotationFieldReferenceFinder
      .findReferencesTo(List.of(conflictingUsedA, conflictingUsedB, conflictingUnusedA, conflictingUnusedB, nonConflictingUsed, nonConflictingUnused));
    method.accept(finder);

    assertThat(finder.fieldsNotReferencedInAnnotation())
      .describedAs("""
        When multiple fields with conflicting names are processed by AnnotationsFieldReferenceFinder::findReferencesTo, we still expect all
        fields that are used in an annotation to be identified as used.
        I.e. they shall not be retuened by fieldsNotReferencedInAnnotation().

        However, for conflicting fields that are not used, we expect only one of them to be returned (specifically the last one encountered).
        Non-conflicting fields shall be processed as usual.
        """)
      .containsExactlyInAnyOrder(conflictingUnusedB, nonConflictingUnused);

    assertThat(logTester.logs())
      .containsExactlyInAnyOrder("""
        Duplicate field name detected: conflictingUsed in Owner.
        This may happen for non-compiling sources and detection of unused variables may be impacted.
        """, """
        Duplicate field name detected: conflictingUnused in Owner.
        This may happen for non-compiling sources and detection of unused variables may be impacted.
        """);
  }

  private static VariableTree field(String simpleName) {
    var ownerSymbol = mock(Symbol.class);
    doReturn("Owner").when(ownerSymbol).name();

    var symbol = mock(Symbol.class);
    doReturn(ownerSymbol).when(symbol).owner();

    var identifierTree = new IdentifierTreeImpl(new InternalSyntaxToken(0, 0, simpleName, List.of(), false));
    var variableTree = spy(new VariableTreeImpl(identifierTree));
    doReturn(symbol).when(variableTree).symbol();

    return variableTree;
  }

  private static LiteralTree literal(String unquotedValue) {
    return new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, new InternalSyntaxToken(0, 0, "\"%s\"".formatted(unquotedValue), List.of(), false));
  }

  private static MethodTree methodWithAnnotationArgs(ExpressionTree... annotationArgs) {
    var annotationType = mock(Type.class);
    doReturn("FieldSource").when(annotationType).name();
    var annotationTypeExpression = spy(new IdentifierTreeImpl(new InternalSyntaxToken(0, 0, "FieldSource", List.of(), false)));
    doReturn(annotationType).when(annotationTypeExpression).symbolType();

    var annotationArgsTree = ArgumentListTreeImpl.emptyList();
    annotationArgsTree.addAll(Arrays.asList(annotationArgs));

    var annotation = new AnnotationTreeImpl(
      new InternalSyntaxToken(0, 0, "@", List.of(), false),
      annotationTypeExpression,
      annotationArgsTree);

    var methodModifiers = new ModifiersTreeImpl(List.of(annotation));
    var parameters = new FormalParametersListTreeImpl(null, null);
    var method = new MethodTreeImpl(
      null,
      new IdentifierTreeImpl(new InternalSyntaxToken(0, 0, "myMethod", List.of(), false)),
      parameters,
      null,
      QualifiedIdentifierListTreeImpl.emptyList(),
      null,
      null);
    method.completeWithModifiers(methodModifiers);

    annotation.setParent(methodModifiers);
    methodModifiers.setParent(method);

    return method;
  }
}
