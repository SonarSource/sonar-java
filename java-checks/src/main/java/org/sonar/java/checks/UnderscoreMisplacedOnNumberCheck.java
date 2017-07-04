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
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S3937")
public class UnderscoreMisplacedOnNumberCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.INT_LITERAL, Kind.LONG_LITERAL);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literalTree = (LiteralTree) tree;
    String value = literalTree.value();
    if (hasIrregularPattern(value) ) {
      reportIssue(literalTree, "Review this number; its irregular pattern indicates an error.");
    }
  }

  private static boolean hasIrregularPattern(String literalValue) {
    List<String> groups = Arrays.asList(cleanup(literalValue).split("_"));
    // groups empty or of size one does not contain "_"
    return groups.size() > 1
      && groups.subList(1, groups.size()).stream()
      .map(String::length)
      .distinct()
      .count() != 1;
  }

  private static String cleanup(String literalValue) {
    String result = literalValue;
    if(result.startsWith("0b") || result.startsWith("0x")) {
      result = result.substring(2);
    }
    if(result.endsWith("L")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

}
