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
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Rule(key = "S2148")
public class UnderscoreOnNumberCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private enum Base {
    BINARY("0b", 9),
    OCTAL("0", 9),
    HEXADECIMAL("0x", 9),
    DECIMAL("", 6);

    private final String prefix;
    private final int minimalLength;

    Base(String prefix, int minimalLength) {
      this.prefix = prefix;
      this.minimalLength = minimalLength;
    }

    private static final Base ofLiteralValue(String literalValue) {
      if (BINARY.isFromBase(literalValue)) {
        return BINARY;
      } else if (HEXADECIMAL.isFromBase(literalValue)) {
        return HEXADECIMAL;
      } else if (OCTAL.isFromBase(literalValue)) {
        return OCTAL;
      }
      return DECIMAL;
    }

    private boolean isFromBase(String value) {
      return value.toLowerCase(Locale.ENGLISH).startsWith(prefix);
    }
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literalTree = (LiteralTree) tree;
    String value = literalTree.value();
    if (!containsUnderscore(value) && !isSerialVersionUID(tree) && shouldUseUnderscore(value)) {
      reportIssue(literalTree, "Add underscores to this numeric value for readability");
    }
  }

  private static boolean containsUnderscore(String literalValue) {
    return literalValue.indexOf('_') >= 0;
  }

  private static boolean isSerialVersionUID(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null && !parent.is(Tree.Kind.VARIABLE)) {
      parent = parent.parent();
    }
    return parent != null && "serialVersionUID".equals(((VariableTree) parent).simpleName().name());
  }

  private static boolean shouldUseUnderscore(String literalValue) {
    String value = LiteralUtils.trimLongSuffix(literalValue);
    Base base = Base.ofLiteralValue(value);
    return value.length() >= (base.minimalLength + base.prefix.length());
  }
}
