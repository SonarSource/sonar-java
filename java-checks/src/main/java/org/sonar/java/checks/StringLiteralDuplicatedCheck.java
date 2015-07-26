/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Map;

@Rule(
  key = "S1192",
  name = "String literals should not be duplicated",
  tags = {"design"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleLinearWithOffsetRemediation(coeff = "2min", offset = "2min", effortToFixDescription = "per duplicate instance" )
public class StringLiteralDuplicatedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_THRESHOLD = 3;

  private static final Integer MINIMAL_LITERAL_LENGTH = 7;
  @RuleProperty(
      key = "threshold",
      description = "Number of times a literal must be duplicated to trigger an issue",
      defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  private final Map<String, LiteralTree> firstOccurrence = Maps.newHashMap();
  private final Multiset<String> occurences = HashMultiset.create();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    firstOccurrence.clear();
    occurences.clear();
    scan(context.getTree());
    for (String literal : occurences.elementSet()) {
      int literalOccurence = occurences.count(literal);
      if (literalOccurence >= threshold) {
        context.addIssue(firstOccurrence.get(literal), this,
            "Define a constant instead of duplicating this literal " + literal + " " + literalOccurence + " times.",
            (double) literalOccurence);
      }
    }
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if(tree.is(Tree.Kind.STRING_LITERAL))  {
      String literal =tree.value();
      if (literal.length() >= MINIMAL_LITERAL_LENGTH) {
        if (!firstOccurrence.containsKey(literal)) {
          firstOccurrence.put(literal, tree);
        }
        occurences.add(literal);
      }
    }
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    //Ignore literals within annotation
  }
}
