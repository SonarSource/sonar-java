/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.checks.helpers.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests an edge case of {@link AnnotationFieldReferenceFinder} that can not be covered with regular Java files by the tests in
 * {@link org.sonar.java.checks.unused.UnusedPrivateFieldCheckTest}.
 */
class AnnotationFieldReferenceFinderTest {
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.ERROR);

  @Test
  void should_graciously_handle_fields_with_overlapping_names() {
    var source = """
      import org.junit.jupiter.params.ParameterizedTest;
      import org.junit.jupiter.params.provider.FieldSource;

      class A {
          Object conflictingUsed;
          Object conflictingUnused;
          Object nonConflictingUsed;
          Object nonConflictingUnused;

          @ParameterizedTest
          @FieldSource({"conflictingUsed", "nonConflictingUsed"})
          void test(int input) {
              // ...
          }
      }
      """;

    // The goal here is to confront AnnotationFieldReferencesFinder with two fields on the same class that have the same name.
    // ECJ will actually not produce such an AST, even if two fields with the same name appear in the code.
    // Still, in case this does happen at some point, we want this test to confirm that we still behave in a safe way.
    //
    // Hence, we will do a hack here by parsing the same source twice, collecting the fields from it, and manually construct a list of
    // fields that contains two different VariableTree instances with the same simpleName.
    var firstParsing = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
    var secondParsing = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);

    var conflictingUsedA = extractField(firstParsing, "conflictingUsed");
    var conflictingUsedB = extractField(secondParsing, "conflictingUsed");
    var conflictingUnusedA = extractField(firstParsing, "conflictingUnused");
    var conflictingUnusedB = extractField(secondParsing, "conflictingUnused");
    var nonConflictingUsed = extractField(firstParsing, "nonConflictingUsed");
    var nonConflictingUnused = extractField(firstParsing, "nonConflictingUnused");

    var finder = AnnotationFieldReferenceFinder
      .findReferencesTo(List.of(conflictingUsedA, conflictingUsedB, conflictingUnusedA, conflictingUnusedB, nonConflictingUsed, nonConflictingUnused));
    firstParsing.accept(finder);

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
        Duplicate field name detected: conflictingUsed in A.
        This may happen for non-compiling sources and detection of unused variables may be impacted.
        """, """
        Duplicate field name detected: conflictingUnused in A.
        This may happen for non-compiling sources and detection of unused variables may be impacted.
        """);
  }

  private VariableTree extractField(JavaTree.CompilationUnitTreeImpl cut, String fieldName) {
    var firstType = (ClassTree) cut.types().get(0);

    return firstType
      .members()
      .stream()
      .filter(member -> member instanceof VariableTree field && fieldName.equals(field.simpleName().name()))
      .findFirst()
      .map(VariableTreeImpl.class::cast)
      .orElseThrow();
  }
}
