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

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = "S1186")
public class EmptyMethodsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
      List<Tree> members = classTree.members();
      checkMethods(members);
      checkSingleNoArgPublicConstructor(members);
    }
  }

  private void checkMethods(List<Tree> members) {
    members.stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .forEach(this::checkMethod);
  }

  private void checkSingleNoArgPublicConstructor(List<Tree> members) {
    List<MethodTree> constructors = members.stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList());
    if (constructors.size() == 1 && isPublicNoArgConstructor(constructors.get(0))) {
      checkMethod(constructors.get(0));
    }
  }

  private static boolean isPublicNoArgConstructor(MethodTree constructor) {
    return ModifiersUtils.hasModifier(constructor.modifiers(), Modifier.PUBLIC) && constructor.parameters().isEmpty();
  }

  private void checkMethod(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null && isEmpty(block) && !containsComment(block)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(methodTree.simpleName())
        .withMessage("Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or complete the implementation.")
        .withQuickFix(() -> computeQuickFix(methodTree))
        .report();
    }
  }

  private static boolean isEmpty(BlockTree block) {
    List<StatementTree> body = block.body();
    return body.isEmpty() || body.stream().allMatch(stmt -> stmt.is(Kind.EMPTY_STATEMENT));
  }

  private static boolean containsComment(BlockTree block) {
    return !block.closeBraceToken().trivias().isEmpty();
  }

  private static JavaQuickFix computeQuickFix(MethodTree method) {
    String commentFormat;
    if (method.block().openBraceToken().range().start().line() == method.block().closeBraceToken().range().start().line()) {
      commentFormat = " /* TODO document why this %s is empty */ ";
    } else {
      String methodPadding = computePadding(method);
      commentFormat = "\n" + methodPadding + "  // TODO document why this %s is empty\n" + methodPadding;
    }

    String comment = String.format(commentFormat, method.is(Kind.CONSTRUCTOR) ? "constructor" : "method");

    AnalyzerMessage.TextSpan textSpan = AnalyzerMessage.textSpanBetween(
      method.block().openBraceToken(), false,
      method.block().closeBraceToken(), false
    );

    return JavaQuickFix.newQuickFix("Insert placeholder comment")
      .addTextEdit(JavaTextEdit.replaceTextSpan(textSpan, comment))
      .build();
  }

  private static String computePadding(MethodTree method) {
    int spaces = method.firstToken().range().start().columnOffset();
    // This loop and return call can be replaced with a call to " ".repeat(spaces) in Java 11
    StringBuilder padding = new StringBuilder("");
    for (int i = 0; i < spaces; i++) {
      padding.append(" ");
    }
    return padding.toString();
  }
}
