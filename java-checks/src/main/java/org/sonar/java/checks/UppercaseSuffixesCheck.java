/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S818")
public class UppercaseSuffixesCheck extends IssuableSubscriptionVisitor {

  @RuleProperty(
    key = "checkOnlyLong",
    description = "Set to \"true\" to ignore \"float\" and \"double\" declarations.",
    defaultValue = "false")
  public boolean checkOnlyLong = false;

  private static final char LONG = 'l';
  private static final char DOUBLE = 'd';
  private static final char FLOAT = 'f';

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.LONG_LITERAL);
  }

  @Override
  public void visitNode(Tree tree) {
    String value = ((LiteralTree) tree).value();
    char suffix = value.charAt(value.length() - 1);
    switch (suffix) {
      case DOUBLE,
        FLOAT:
        if (checkOnlyLong) {
          return;
        }
        // fall through
      case LONG:
        reportIssue(tree, "Upper-case this literal \"" + suffix + "\" suffix.");
        break;
      default:
        // do nothing
    }
  }
}
