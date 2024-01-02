/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.helpers;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomnessDetectorTest {

  @ParameterizedTest
  @MethodSource("stringPerLevelForLanguageScore")
  void test_progressive_language_score_sensibility(String input, int currentSensibility) {
    // The higher the sensibility, the more we filter.
    // We want to test that the current level accept the string, but not the level above.
    RandomnessDetector current = new RandomnessDetector(currentSensibility);
    assertTrue(current.hasLowLanguageScore(input));
    RandomnessDetector above = new RandomnessDetector(currentSensibility + 1);
    assertFalse(above.hasLowLanguageScore(input));
  }

  @Test
  void test_last_level_language_score_sensibility() {
    RandomnessDetector current = new RandomnessDetector(10);
    assertFalse(current.hasLowLanguageScore("xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx"));
  }

  private static Stream<Arguments> stringPerLevelForLanguageScore() {
    return Stream.of(
      Arguments.of("four/four/four/four/four/four/four/four/four/four", 0),
      Arguments.of("four/four/four/four/four/four/four/four/four/xwwx", 1),
      Arguments.of("four/four/four/four/four/four/four/four/xwwx/xwwx", 2),
      Arguments.of("four/four/four/four/four/four/four/xwwx/xwwx/xwwx", 3),
      Arguments.of("four/four/four/four/four/four/xwwx/xwwx/xwwx/xwwx", 4),
      Arguments.of("four/four/four/four/four/xwwx/xwwx/xwwx/xwwx/xwwx", 5),
      Arguments.of("four/four/four/four/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx", 6),
      Arguments.of("four/four/four/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx", 7),
      Arguments.of("four/four/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx", 8),
      Arguments.of("four/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx/xwwx", 9)
    );
  }

  @ParameterizedTest
  @MethodSource("stringPerLevelForEntropyScore")
  void test_progressive_entropy_score_sensibility(String input, int currentSensibility) {
    // The higher the sensibility, the more we filter.
    // We want to test that the current level accept the string, but not the level above.
    RandomnessDetector current = new RandomnessDetector(currentSensibility);
    assertTrue(current.hasEnoughEntropy(input));
    RandomnessDetector above = new RandomnessDetector(currentSensibility + 1);
    assertFalse(above.hasEnoughEntropy(input));
  }

  @Test
  void test_last_level_entropy_sensibility() {
    RandomnessDetector current = new RandomnessDetector(10);
    assertFalse(current.hasEnoughEntropy("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567"));
  }

  private static Stream<Arguments> stringPerLevelForEntropyScore() {
    return Stream.of(
      Arguments.of("____________________________________________________________", 0),
      Arguments.of("abcdef______________________________________________________", 1),
      Arguments.of("abcdefghijkl________________________________________________", 2),
      Arguments.of("abcdefghijklmnopqr__________________________________________", 3),
      Arguments.of("abcdefghijklmnopqrstuvwx____________________________________", 4),
      Arguments.of("abcdefghijklmnopqrstuvwxyzABCD______________________________", 5),
      Arguments.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJ________________________", 6),
      Arguments.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP__________________", 7),
      Arguments.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV____________", 8),
      Arguments.of("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01______", 9)
    );
  }

  @Test
  void test_is_random_high_sensibility() {
    String str = "xxx_xxx_xxx_xxx_xxx_xxx_xxx_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    System.out.println(ShannonEntropy.calculate(str));
    System.out.println(LatinAlphabetLanguagesHelper.humanLanguageScore(str));
    RandomnessDetector current = new RandomnessDetector(7);
    // Low entropy, high language score
    assertFalse(current.isRandom("the_the_the_the_the_the_the"));
    // Low entropy, low language score
    assertFalse(current.isRandom("xxx_xxx_xxx_xxx_xxx_xxx_xxx"));
    // High entropy, high language score
    assertFalse(current.isRandom("the_the_the_the_the_the_the_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"));
    // High entropy and low language score
    assertTrue(current.isRandom("xxx_xxx_xxx_xxx_xxx_xxx_xxx_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"));
  }

  @Test
  void test_is_random_low_sensibility() {
    String str = "my package";
    System.out.println(ShannonEntropy.calculate(str));
    System.out.println(LatinAlphabetLanguagesHelper.humanLanguageScore(str));
    RandomnessDetector current = new RandomnessDetector(1);
    assertTrue(current.isRandom("my package"));
    assertTrue(current.isRandom("xxx_xxx_xxx_xxx_xxx_xxx_xxx"));
    assertTrue(current.isRandom("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"));
    // Language score is so high that we still consider it as not random
    assertFalse(current.isRandom("the_the_the_the_the_the_the"));
    // Entropy is so low that we still consider it as not random
    assertFalse(current.isRandom("xxxxxxxxxxxx"));
  }

}
