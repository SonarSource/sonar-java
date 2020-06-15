/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.regex.ast.Location;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.plugins.java.api.JavaCheck;

/**
 * Marker interface for rules targeting regexes
 */
public interface RegexCheck extends JavaCheck {

  /**
   * Issue location holder, replacing regex syntax elements into text spans for reporting
   */
  public static class RegexIssueLocation {

    private final List<AnalyzerMessage.TextSpan> locations;
    private final String message;

    public RegexIssueLocation(RegexSyntaxElement tree, String message) {
      this.locations = textSpansFromRegexSyntaxElement(tree);
      this.message = message;
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
      return locations.stream()
        .map(loc -> new RegexIssueLocation(loc, message))
        .collect(Collectors.toList());
    }

    private static List<AnalyzerMessage.TextSpan> textSpansFromRegexSyntaxElement(RegexSyntaxElement tree) {
      List<Location> locs = tree.getLocations().stream()
        .filter(location -> !location.isEmpty())
        .collect(Collectors.toList());
      if (locs.isEmpty()) {
        // contains only empty locations, take the first one
        locs = Collections.singletonList(tree.getLocations().get(0));
      }
      return locs.stream().map(location -> {
          AnalyzerMessage.TextSpan result = AnalyzerMessage.textSpanFor(location.getJavaTree());
          return new AnalyzerMessage.TextSpan(
            result.startLine,
            // Adding 1 to handle beginning of the String with quote
            result.startCharacter + location.getBeginningOffset() + 1,
            result.endLine,
            // Adding 1 to handle beginning of the String with quote
            result.startCharacter + location.getEndingOffset() + 1 + (location.isEmpty() ? 1 : 0));
        }).collect(Collectors.toList());
    }
  }

}
