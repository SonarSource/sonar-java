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
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.helpers.AnnotationsHelper.annotationTypeIdentifier;

class AnnotationsHelperTest {
  @Test
  void testAnnotationTypeIdentifier() {
    assertThat(annotationTypeIdentifier("noDot")).isEqualTo("noDot");
    assertThat(annotationTypeIdentifier("one.dot")).isEqualTo("dot");
    assertThat(annotationTypeIdentifier("many.many.dots")).isEqualTo("dots");
  }
}
