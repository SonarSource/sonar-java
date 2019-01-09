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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1192")
public class StringLiteralDuplicatedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_THRESHOLD = 3;

  // String literals include quotes, so this means length 5 as defined in RSPEC
  private static final int MINIMAL_LITERAL_LENGTH = 7;

  @RuleProperty(
    key = "threshold",
    description = "Number of times a literal must be duplicated to trigger an issue",
    defaultValue = "" + DEFAULT_THRESHOLD)
  public int threshold = DEFAULT_THRESHOLD;

  private final Multimap<String, LiteralTree> occurrences = ArrayListMultimap.create();
  private final Map<String, VariableTree> constants = new HashMap<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    occurrences.clear();
    constants.clear();
    scan(context.getTree());
    for (String entry : occurrences.keySet()) {
      Collection<LiteralTree> literalTrees = occurrences.get(entry);
      int literalOccurrence = literalTrees.size();
      if (constants.containsKey(entry)) {
        VariableTree constant = constants.get(entry);
        List<LiteralTree> duplications = literalTrees.stream().filter(literal -> literal.parent() != constant).collect(Collectors.toList());
        context.reportIssue(this, duplications.iterator().next(),
          "Use already-defined constant '" + constant.simpleName() + "' instead of duplicating its value here.",
          secondaryLocations(duplications.subList(1, duplications.size())), literalOccurrence);
      } else if (literalOccurrence >= threshold) {
        context.reportIssue(
          this,
          literalTrees.iterator().next(),
          "Define a constant instead of duplicating this literal " + entry + " " + literalOccurrence + " times.",
          secondaryLocations(literalTrees), literalOccurrence);
      }
    }
  }

  private static List<JavaFileScannerContext.Location> secondaryLocations(Collection<LiteralTree> literalTrees) {
    return literalTrees.stream().map(element -> new JavaFileScannerContext.Location("Duplication", element)).collect(Collectors.toList());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      String literal = tree.value();
      if (literal.length() >= MINIMAL_LITERAL_LENGTH) {
        occurrences.put(literal, tree);
      }
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    ExpressionTree initializer = tree.initializer();
    if (initializer != null && initializer.is(Tree.Kind.STRING_LITERAL)
      && ModifiersUtils.hasModifier(tree.modifiers(), Modifier.STATIC)
      && ModifiersUtils.hasModifier(tree.modifiers(), Modifier.FINAL)) {
      constants.putIfAbsent(((LiteralTree) initializer).value(), tree);
      return;
    }
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if(ModifiersUtils.hasModifier(tree.modifiers(), Modifier.DEFAULT)) {
      //Ignore default methods to avoid catch-22 with S1214
      return;
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    //Ignore literals within annotation
  }
}
