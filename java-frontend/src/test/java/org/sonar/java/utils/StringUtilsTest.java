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
package org.sonar.java.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringUtilsTest {
  @Test
  void testIsEmpty() {
    assertThat(StringUtils.isEmpty(null)).isTrue();
    assertThat(StringUtils.isEmpty("")).isTrue();
    assertThat(StringUtils.isEmpty(" ")).isFalse();
    assertThat(StringUtils.isEmpty("abc")).isFalse();
  }

  @Test
  void testCountMatches() {
    assertThat(StringUtils.countMatches(null, "ab")).isZero();
    assertThat(StringUtils.countMatches("", "ab")).isZero();
    assertThat(StringUtils.countMatches("ab", null)).isZero();
    assertThat(StringUtils.countMatches("ab", "")).isZero();

    assertThat(StringUtils.countMatches("abababab", "cccc")).isZero();
    assertThat(StringUtils.countMatches("abababab", "ab")).isEqualTo(4);

    assertThat(StringUtils.countMatches("abaTaba", "aba")).isEqualTo(2);
    assertThat(StringUtils.countMatches("abababa", "aba")).isEqualTo(2);
  }

  @Test
  void testFlatten() {
    assertThat(StringUtils.flatten()).isEmpty();
    assertThat(StringUtils.flatten("a", "b", "c")).containsExactly("a", "b", "c");

    assertThat(StringUtils.flatten(new String[] {"a", "b"}, "c")).containsExactly("a", "b", "c");
    assertThat(StringUtils.flatten("a", new String[] {"b", "c"})).containsExactly("a", "b", "c");

    assertThat(StringUtils.flatten(List.of("A", "B"), "a", new String[] {"b", "c"}))
      .containsExactly("A", "B", "a", "b", "c");
  }

  @Test
  void testFlatten_exceptions() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.flatten("a", 2))
      .withMessageContaining("Unsupported argument type:");

    assertThatIllegalArgumentException()
      .isThrownBy(() -> StringUtils.flatten((Object) new int[]{4, 5}))
      .withMessageContaining("Unsupported argument type:");

    var list = List.of("b", List.of("c", "d"));
    assertThatThrownBy(() -> StringUtils.flatten("a", list))
      .isInstanceOf(ArrayStoreException.class);
  }

  @Test
  void testTokenizeIdentifier() {
    assertThat(StringUtils.tokenizeIdentifier("userPassword")).containsExactly("user", "password");
    assertThat(StringUtils.tokenizeIdentifier("user_token")).containsExactly("user", "token");
    assertThat(StringUtils.tokenizeIdentifier("HMAC")).containsExactly("hmac");

    // Digit-suffixed acronyms also yield their bare letter-only form.
    assertThat(StringUtils.tokenizeIdentifier("AES256_KEY")).containsExactly("aes256", "aes", "key");
    assertThat(StringUtils.tokenizeIdentifier("RSA2048")).containsExactly("rsa2048", "rsa");
    assertThat(StringUtils.tokenizeIdentifier("HMAC256")).containsExactly("hmac256", "hmac");

    // Digit-bearing words are kept whole, so they still match their own literal spelling.
    assertThat(StringUtils.tokenizeIdentifier("pbkdf2")).containsExactly("pbkdf2", "pbkdf");
    assertThat(StringUtils.tokenizeIdentifier("poly1305")).containsExactly("poly1305", "poly");
    assertThat(StringUtils.tokenizeIdentifier("PBKDF2")).containsExactly("pbkdf2", "pbkdf");

    // A word right after a digit run in a compound acronym must be isolated on its own.
    assertThat(StringUtils.tokenizeIdentifier("SHA256RSA")).contains("sha", "rsa");

    // camelCase digit suffix: each camelCase part is kept whole (`cha20`) and its letter-only
    // form is added as an extra candidate (`cha`). This never produces a single `chacha20`
    // token, so it does not match the `chacha20` keyword even though ChaCha20 is how the
    // algorithm is usually written. This is a known, accepted heuristic gap: matching camelCase
    // concatenations would also make randomBytes match randombytes, which is deliberately not
    // supported (see splitRandomBytes in PseudoRandomCheckSecurityKeywordsSample.java).
    assertThat(StringUtils.tokenizeIdentifier("ChaCha20")).containsExactly("cha", "cha20", "cha");
  }
}
