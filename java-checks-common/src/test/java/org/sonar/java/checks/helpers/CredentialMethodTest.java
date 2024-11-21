/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialMethodTest {
  @Test
  void isConstructor() {
    var constructor = new CredentialMethod("Object", "Object", Collections.emptyList(), Collections.emptyList());
    assertThat(constructor.isConstructor()).isTrue();
    var nonConstructor = new CredentialMethod("Object", "equals", Collections.emptyList(), Collections.emptyList());
    assertThat(nonConstructor.isConstructor()).isFalse();
    var stringConstructor = new CredentialMethod("java.lang.String", "String", Collections.emptyList(), Collections.emptyList());
    assertThat(stringConstructor.isConstructor()).isTrue();
    var stringCompareTo = new CredentialMethod("java.lang.String", "compareTo", List.of("java.lang.String"), Collections.emptyList());
    assertThat(stringCompareTo.isConstructor()).isFalse();
    var innerClassConstructor = new CredentialMethod("org.sonar.Outer$Inner", "Inner", Collections.emptyList(), Collections.emptyList());
    assertThat(innerClassConstructor.isConstructor()).isTrue();
  }

  @Test
  void methodMatcher_is_recycled() {
    var constructor = new CredentialMethod("Object", "Object", Collections.emptyList(), Collections.emptyList());
    MethodMatchers constructorMatcher = constructor.methodMatcher();
    assertThat(constructor.methodMatcher()).isSameAs(constructorMatcher);
    var equalsMatcher = new CredentialMethod("Object", "equals", Collections.emptyList(), Collections.emptyList());
    MethodMatchers methodMatcher = equalsMatcher.methodMatcher();
    assertThat(equalsMatcher.methodMatcher()).isSameAs(methodMatcher);
  }
}
