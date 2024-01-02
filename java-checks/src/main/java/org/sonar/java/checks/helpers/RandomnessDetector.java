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

import org.sonar.java.annotations.VisibleForTesting;

public class RandomnessDetector {
  private static final int MIN_SECRET_LENGTH_FOR_GIVEN_ENTROPY = 25;
  private static final double ENTROPY_INCREASE_FACTOR_BY_MISSING_CHARACTER = 1.034;

  private final double minEntropyThreshold;
  private final double maxLanguageScore;
  private static final double LANGUAGE_SCORE_INCREMENT = 0.3;
  private static final double ENTROPY_SCORE_INCREMENT = 0.6;

  /**
   * Randomness sensibility should be between 0 and 10.
   */
  public RandomnessDetector(double randomnessSensibility) {
    this.minEntropyThreshold = randomnessSensibility * ENTROPY_SCORE_INCREMENT;
    this.maxLanguageScore = (10 - randomnessSensibility) * LANGUAGE_SCORE_INCREMENT;
  }

  public boolean isRandom(String literal) {
    return hasEnoughEntropy(literal) && hasLowLanguageScore(literal);
  }

  @VisibleForTesting
  boolean hasEnoughEntropy(String literal) {
    double effectiveMinEntropyThreshold = minEntropyThreshold;
    if (literal.length() < MIN_SECRET_LENGTH_FOR_GIVEN_ENTROPY) {
      int missingCharacterCount = MIN_SECRET_LENGTH_FOR_GIVEN_ENTROPY - literal.length();
      // increase the entropy threshold constraint when there's not enough characters
      effectiveMinEntropyThreshold *= Math.pow(ENTROPY_INCREASE_FACTOR_BY_MISSING_CHARACTER, missingCharacterCount);
    }
    return ShannonEntropy.calculate(literal) >= effectiveMinEntropyThreshold;
  }

  @VisibleForTesting
  boolean hasLowLanguageScore(String literal) {
    return LatinAlphabetLanguagesHelper.humanLanguageScore(literal) < maxLanguageScore;
  }
}
