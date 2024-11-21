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
package org.sonar.java.regex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.performance.measure.PerformanceMeasure;

public final class RegexCache {
  private final Map<List<LiteralTree>, RegexParseResult> cache = new HashMap<>();

  public RegexParseResult getRegexForLiterals(FlagSet initialFlags, LiteralTree... stringLiterals) {
    return cache.computeIfAbsent(
      Arrays.asList(stringLiterals),
      k -> {
        PerformanceMeasure.Duration regexForLiteralsDuration = PerformanceMeasure.start("RegexParser");
        RegexParseResult result = new RegexParser(new JavaAnalyzerRegexSource(k), initialFlags).parse();
        regexForLiteralsDuration.stop();
        return result;
      });
  }

}
