/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

public class CommentsMatcherHelper {

  private CommentsMatcherHelper() {
  }

  public static Set<String> getBlockTagsFromMethodJavadoc(MethodTree methodTree, Pattern blockTagsPattern, String matchingGroup) {
    return methodTree.firstToken().trivias().stream()
      .map(SyntaxTrivia::comment)
      .map(c -> c.split("\\r?\\n"))
      .flatMap(Arrays::stream)
      .map(blockTagsPattern::matcher)
      .filter(Matcher::matches)
      .map(matcher -> matcher.group(matchingGroup))
      .collect(Collectors.toSet());
  }
}
