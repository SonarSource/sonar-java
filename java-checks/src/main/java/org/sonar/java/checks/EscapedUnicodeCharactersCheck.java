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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2973")
public class EscapedUnicodeCharactersCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> UNICODE_WHITESPACES = SetUtils.immutableSetOf(
    "1680", "2000", "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009", "200A",
    "2028", "2029", "202F", "205F", "3000", "180E", "200B", "200C", "200D", "2060", "FEFF");

  private static final Pattern UNICODE_ESCAPED_CHAR = Pattern.compile("\\\\u+[a-fA-F0-9]{4}");

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.STRING_LITERAL, Kind.TEXT_BLOCK);
  }

  @Override
  public void visitNode(Tree node) {
    if (LiteralUtils.isEmptyString(node)) {
      return;
    }
    String value = LiteralUtils.trimQuotes(((LiteralTree) node).value());
    if (node.is(Kind.TEXT_BLOCK)) {
      value = value.replaceAll("(\r?\n|\r)\\s*", "");
    }
    // replace \\ with nothing just to differentiate \u0000 and \\u0000
    Matcher matcher = UNICODE_ESCAPED_CHAR.matcher(value.replace("\\\\", ""));
    List<String> matches = getAllMatches(matcher);
    if (!matches.isEmpty()) {
      boolean notOnlyUnicodeEscaped = !matcher.replaceAll("").isEmpty();
      if (notOnlyUnicodeEscaped && matches.stream().anyMatch(EscapedUnicodeCharactersCheck::isPrintableEscapedUnicode)) {
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

  private static boolean isPrintableEscapedUnicode(String input) {
    String hexValue = input.substring(input.length() - 4).toUpperCase(Locale.ROOT);
    if (UNICODE_WHITESPACES.contains(hexValue)) {
      return false;
    }
    int unicodePointDecimal = Integer.parseInt(hexValue, 16);
    return (31 < unicodePointDecimal && unicodePointDecimal < 127) || 160 < unicodePointDecimal ;
  }

}
