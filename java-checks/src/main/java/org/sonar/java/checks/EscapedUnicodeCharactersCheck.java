/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(key = "S2973")
public class EscapedUnicodeCharactersCheck extends IssuableSubscriptionVisitor {

  private static final Pattern UNICODE_ESCAPED_CHAR = Pattern.compile("\\\\u+[a-fA-F0-9]{4}");
  private static final Predicate<String> IS_PRINTABLE_ESCAPED_UNICODE = input -> {
    int unicodePointDecimal = Integer.parseInt(input.substring(input.length() - 4), 16);
    return (31 < unicodePointDecimal && unicodePointDecimal < 127) || 160 < unicodePointDecimal ;
  };

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.STRING_LITERAL);
  }

  @Override
  public void visitNode(Tree node) {
    if (LiteralUtils.isEmptyString(node)) {
      return;
    }
    String value = LiteralUtils.trimQuotes(((LiteralTree) node).value());
    // replace \\ with nothing just to differentiate \u0000 and \\u0000
    Matcher matcher = UNICODE_ESCAPED_CHAR.matcher(value.replaceAll("\\\\\\\\",""));
    List<String> matches = getAllMatches(matcher);
    if (!matches.isEmpty()) {
      boolean notOnlyUnicodeEscaped = !matcher.replaceAll("").isEmpty();
      if (notOnlyUnicodeEscaped && matches.stream().anyMatch(IS_PRINTABLE_ESCAPED_UNICODE)) {
        reportIssue(node, "Remove this Unicode escape sequence and use the character instead.");
      }
    }
  }

  private static List<String> getAllMatches(Matcher matcher) {
    List<String> matches = new ArrayList<>();
    while (matcher.find()) {
      matches.add(matcher.group());
    }
    return matches;
  }

}
