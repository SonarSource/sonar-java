/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks.codesnippet;

public class PatternMatcherResult {

  private static final PatternMatcherResult MISMATCH = new PatternMatcherResult(0, null);

  private final boolean isMatching;
  private final int matchingToIndex;
  private final PatternMatcherResult nextPatternMatcherResult;

  public PatternMatcherResult(int matchingToIndex) {
    this(matchingToIndex, null);
  }

  public PatternMatcherResult(int matchingToIndex, PatternMatcherResult nextPatternMatcherResult) {
    this.isMatching = matchingToIndex > 0 && nextPatternMatcherResult == null || nextPatternMatcherResult != null && nextPatternMatcherResult.isMatching();
    this.matchingToIndex = matchingToIndex;
    this.nextPatternMatcherResult = nextPatternMatcherResult;
  }

  public boolean isMatching() {
    return isMatching;
  }

  public int getMatchingToIndex() {
    return matchingToIndex;
  }

  public PatternMatcherResult getNextPatternMatcherResult() {
    return nextPatternMatcherResult;
  }

  public static PatternMatcherResult getMismatch() {
    return MISMATCH;
  }

}
