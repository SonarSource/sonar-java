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
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
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
    return Arrays.asList(Kind.INT_LITERAL, Kind.LONG_LITERAL);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literalTree = (LiteralTree) tree;
    String value = literalTree.value();

    /* Binary numbers are never flagged. Splitting binary numbers into irregular groups may be legitimate where binary numbers are e.g.
    used for encoding a series of flags and different groups have different meanings or are related to different topics/properties. */
    if (isNotABinaryNumber(value) && hasIrregularPattern(value)) {
      reportIssue(literalTree, "Review this number; its irregular pattern indicates an error.");
    }
  }

  private static boolean isNotABinaryNumber(String literalValue) {
    return !literalValue.startsWith("0b") && !literalValue.startsWith("0B");
  }

  private static boolean hasIrregularPattern(String literalValue) {
    List<String> groups = Arrays.asList(cleanup(literalValue).split("_"));
    // groups empty or of size one does not contain "_"
    if (groups.size() <= 1) {
      return false;
    }
    int firstGroupLength = groups.get(0).length();
    List<Integer> lengths = groups.stream().skip(1).map(String::length).distinct().toList();
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
