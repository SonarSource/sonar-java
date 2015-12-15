/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(
  key = "S2973",
  name = "Escaped Unicode characters should not be used",
  priority = Priority.MAJOR,
  tags = {Tag.CONFUSING})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class EscapedUnicodeCharactersCheck extends SubscriptionBaseVisitor {

  private static final Pattern UNICODE_ESCAPED_CHAR = Pattern.compile("\\\\u+[a-fA-F0-9]{4}");
  private static final Predicate<String> IS_PRINTABLE_ESCAPED_UNICODE = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return Integer.parseInt(input.substring(input.length() - 4), 16) > 31;
    }
  };

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.STRING_LITERAL);
  }

  @Override
  public void visitNode(Tree node) {
    String value = LiteralUtils.trimQuotes(((LiteralTree) node).value());
    if (value.isEmpty()) {
      return;
    }
    // replace \\u with \\z just to differentiate \u0000 and \\u0000
    Matcher matcher = UNICODE_ESCAPED_CHAR.matcher(value.replaceAll("\\\\\\\\u", "\\\\z"));
    List<String> matches = getAllMatches(matcher);
    if (!matches.isEmpty()) {
      boolean notOnlyUnicodeEscaped = !matcher.replaceAll("").isEmpty();
      if (notOnlyUnicodeEscaped && Iterables.any(matches, IS_PRINTABLE_ESCAPED_UNICODE)) {
        addIssue(node, "Remove this Unicode escape sequence and use the character instead.");
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
