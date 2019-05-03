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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = "S3937")
public class UnderscoreMisplacedOnNumberCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.INT_LITERAL, Kind.LONG_LITERAL);
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
    if (groups.size() <= 1) {
      return false;
    }
    int firstGroupLength = groups.get(0).length();
    List<Integer> lengths = groups.stream().skip(1).map(String::length).distinct().collect(Collectors.toList());
    return lengths.size() != 1 || lengths.get(0) < firstGroupLength;
  }

  private static String cleanup(String literalValue) {
    String result = literalValue.toLowerCase();
    if(result.startsWith("0b") || result.startsWith("0x")) {
      result = result.substring(2);
    }
    if(result.endsWith("l")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

}
