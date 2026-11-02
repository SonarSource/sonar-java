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
package org.sonar.java.test;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestResourceScopeTest {

  @Test
  void nested_scopes_close_only_their_own_resources_after_assertions() throws Exception {
    List<String> calls = new ArrayList<>();
    try (TestResourceScope.Scope outer = TestResourceScope.open()) {
      TestResourceScope.retain(() -> calls.add("outer"));
      try (TestResourceScope.Scope inner = TestResourceScope.open()) {
        TestResourceScope.retain(() -> calls.add("inner"));
        assertThat(calls).isEmpty();
      }
      assertThat(calls).containsExactly("inner");
    }
    assertThat(calls).containsExactly("inner", "outer");
  }

  @Test
  void scope_closes_every_resource_in_reverse_order_even_when_cleanup_fails() throws Exception {
    TestResourceScope.Scope scope = TestResourceScope.open();
    List<String> calls = new ArrayList<>();
    IllegalStateException first = new IllegalStateException("first");
    Exception second = new Exception("second");
    TestResourceScope.retain(() -> calls.add("last"));
    TestResourceScope.retain(() -> { calls.add("second"); throw second; });
    TestResourceScope.retain(() -> { calls.add("first"); throw first; });
    assertThatThrownBy(scope::close).isSameAs(first).hasSuppressedException(second);
    scope.close();
    assertThat(calls).containsExactly("first", "second", "last");
  }

  @Test
  void scope_cleanup_attempts_every_resource_and_suppresses_errors() throws Exception {
    TestResourceScope.Scope scope = TestResourceScope.open();
    List<String> calls = new ArrayList<>();
    AssertionError failure = new AssertionError("cleanup");
    TestResourceScope.retain(() -> calls.add("last"));
    TestResourceScope.retain(() -> { throw failure; });
    Exception first = new Exception("first");
    TestResourceScope.retain(() -> { throw first; });

    assertThatThrownBy(scope::close).isSameAs(first).hasSuppressedException(failure);
    scope.close();
    assertThat(calls).containsExactly("last");
    try (TestResourceScope.Scope next = TestResourceScope.open()) {
      TestResourceScope.retain(() -> calls.add("next"));
    }
    assertThat(calls).containsExactly("last", "next");
  }

  @Test
  void scope_cleanup_rethrows_first_error_after_attempting_every_resource() throws Exception {
    TestResourceScope.Scope scope = TestResourceScope.open();
    List<String> calls = new ArrayList<>();
    AssertionError first = new AssertionError("first");
    Exception second = new Exception("second");
    TestResourceScope.retain(() -> calls.add("last"));
    TestResourceScope.retain(() -> { throw second; });
    TestResourceScope.retain(() -> { throw first; });
    TestResourceScope.retain(() -> { throw first; });

    assertThatThrownBy(scope::close).isSameAs(first).hasSuppressedException(second);
    scope.close();
    assertThat(calls).containsExactly("last");
  }

  @Test
  void missing_scope_fails_loudly_and_closes_the_unretained_fixture() {
    List<String> calls = new ArrayList<>();
    assertThatThrownBy(() -> TestResourceScope.retain(() -> calls.add("closed")))
      .isInstanceOf(IllegalStateException.class).hasMessageContaining("@TestResourceScope.Scoped");
    assertThat(calls).containsExactly("closed");
  }

  @Test
  void missing_scope_preserves_cleanup_exceptions_as_suppressed() {
    Exception failure = new Exception("cleanup failed");
    assertThatThrownBy(() -> TestResourceScope.retain(() -> { throw failure; }))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("@TestResourceScope.Scoped")
      .hasSuppressedException(failure);
  }

  @Test
  void missing_scope_propagates_cleanup_errors() {
    AssertionError failure = new AssertionError("cleanup failed");
    assertThatThrownBy(() -> TestResourceScope.retain(() -> { throw failure; }))
      .isSameAs(failure);
  }
}
