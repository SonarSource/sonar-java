/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Note that {@link org.sonar.squidbridge.checks.AbstractNoSonarCheck} can't be used because of bug SSLRSQBR-16.
 */
@Rule(key = "NoSonar")
@RspecKey("S1291")
public class NoSonarCheck extends IssuableSubscriptionVisitor {

  private static final String PATTERN = "NOSONAR";
  private static final String PATTERN_ONLY_WHEN_NO_DETAILS = "^[/\\*\\s]*NOSONAR[/\\*\\s]*$";
  private static final String MESSAGE = "Is //NOSONAR used to exclude false-positive or to hide real quality flaw ?";

  private final CommentContainsPatternChecker checker = new CommentContainsPatternChecker(this, PATTERN, MESSAGE);
  private final Pattern noDetailsOnlyChecker = Pattern.compile(PATTERN_ONLY_WHEN_NO_DETAILS);

  @RuleProperty(
    key = "onlyWhenNoDetailsProvided",
    description = "Only raise an issue when //NOSONAR is used alone, without further text (expected to describe why it has been added)",
    defaultValue = "false")
  protected boolean onlyWhenNoDetailsProvided = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    if (onlyWhenNoDetailsProvided) {
      if (noDetailsOnlyChecker.matcher(syntaxTrivia.comment()).matches()) {
        addIssue(syntaxTrivia.startLine(), MESSAGE);
      }
    } else {
      checker.checkTrivia(syntaxTrivia);
    }
  }

}
