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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2479")
public class ControlCharacterInLiteralCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_FORMAT = "Remove the non-escaped \\u%04X character from this literal.";

  private static final String CONTROL_CHARACTERS =
    // ASCII control character, C0 control characters
    "\u0000-\u0008" +
    // skip U+000A line feed and U+000B line tabulation tab
    "\u000C" +
    // skip U+000D carriage return
    "\u000E-\u001F" +
    // stop before U+0020 space and include U+007F(delete) U+0085(next line) U+00A0(no-break space)
    "\u007F\u0085\u00A0" +
    // Unicode Whitespace > U+007F
    "\u1680\u180E\u2000-\u200D\u2028\u2029\u202F\u205F\u2060\u3000\uFEFF";

  // line tab, vertical tab, character tabulation set, character tabulation with justification, line tabulation set
  private static final String TAB_CHARACTERS = "\u0009\u000B\u0088\u0089\u008A";

  private static final Pattern CONTROL_CHARACTERS_PATTERN = Pattern.compile("[" + CONTROL_CHARACTERS + TAB_CHARACTERS + "]");
  private static final Pattern CONTROL_CHARACTERS_WITHOUT_TABS_PATTERN = Pattern.compile("[" + CONTROL_CHARACTERS + "]");

  @RuleProperty(
    key = "allowTabsInTextBlocks",
    description = "Allow tabs in text blocks",
    defaultValue = "false")
  public boolean allowTabsInTextBlocks;

  public ControlCharacterInLiteralCheck() {
    this.allowTabsInTextBlocks = false;
  }


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.STRING_LITERAL, Tree.Kind.CHAR_LITERAL, Tree.Kind.TEXT_BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literal = (LiteralTree) tree;
    String literalValue = LiteralUtils.getAsStringValue(literal);
    Matcher matcher = null;
    if (allowTabsInTextBlocks && tree.is(Tree.Kind.TEXT_BLOCK)) {
      matcher = CONTROL_CHARACTERS_WITHOUT_TABS_PATTERN.matcher(literalValue);
    } else {
      matcher = CONTROL_CHARACTERS_PATTERN.matcher(literalValue);
    }
    if (matcher.find()) {
      reportIssue(literal,  String.format(MESSAGE_FORMAT, literalValue.codePointAt(matcher.start())));
    }
  }

}
