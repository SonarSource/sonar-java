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
package org.sonar.java.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.regex.ast.IndexRange;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;

/**
 * Marker interface for rules targeting regexes
 */
public interface RegexCheck extends JavaCheck {

  /**
   * Issue location holder, replacing regex syntax elements into text spans for reporting
   */
  class RegexIssueLocation {

    private static final String CONTINUATION_MESSAGE = "Continuing here";
    private final List<AnalyzerMessage.TextSpan> locations;
    private final String message;

    public RegexIssueLocation(RegexSyntaxElement tree, String message) {
      this.locations = ((JavaAnalyzerRegexSource) tree.getSource()).textSpansFor(tree.getRange());
      this.message = message;
    }

    public RegexIssueLocation(List<RegexSyntaxElement> trees, String message) {
      this.locations = textSpansFromRegexSyntaxElements(trees);
      this.message = message;
    }

    public static RegexIssueLocation fromCommonsRegexIssueLocation(org.sonarsource.analyzer.commons.regex.RegexIssueLocation location) {
      return new RegexIssueLocation(location.syntaxElements(), location.message());
    }

    private RegexIssueLocation(AnalyzerMessage.TextSpan location, String message) {
      this.locations = Collections.singletonList(location);
      this.message = message;
    }

    public List<AnalyzerMessage.TextSpan> locations() {
      return locations;
    }

    public String message() {
      return message;
    }

    public List<RegexIssueLocation> toSingleLocationItems() {
      if (locations.size() == 1) {
        return Collections.singletonList(this);
      }
      return Stream.concat(
        Stream.of(new RegexIssueLocation(locations.get(0), message)),
        locations.stream().skip(1).map(loc -> new RegexIssueLocation(loc, CONTINUATION_MESSAGE)))
        .collect(Collectors.toList());
    }

    private static List<AnalyzerMessage.TextSpan> textSpansFromRegexSyntaxElements(List<RegexSyntaxElement> trees) {
      JavaAnalyzerRegexSource source = (JavaAnalyzerRegexSource) trees.get(0).getSource();
      List<AnalyzerMessage.TextSpan> locations = new ArrayList<>();
      IndexRange current = null;
      for (RegexSyntaxElement tree : trees) {
        if (current == null) {
          current = tree.getRange();
        } else if (tree.getRange().getBeginningOffset() == current.getEndingOffset()) {
          current = new IndexRange(current.getBeginningOffset(), tree.getRange().getEndingOffset());
        } else {
          locations.addAll(source.textSpansFor(current));
          current = tree.getRange();
        }
      }
      if (current != null) {
        locations.addAll(source.textSpansFor(current));
      }
      return locations;
    }
  }

}
