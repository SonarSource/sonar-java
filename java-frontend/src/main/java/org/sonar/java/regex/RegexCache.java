/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
