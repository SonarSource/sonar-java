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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1186")
public class EmptyMethodsCheck extends ExtendedIssueBuilderSubscriptionVisitor {

  // Some methods may legitimately be left empty, e.g. methods annotated with org.aspectj.lang.annotation.Pointcut. We ignore them here.
  private static final String IGNORED_METHODS_ANNOTATION = "org.aspectj.lang.annotation.Pointcut";
  private static final String IGNORED_METHODS_ANNOTATION_UNQUALIFIED = "Pointcut";

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
      checkConstructors(members);
    }
  }

  private void checkMethods(List<Tree> members) {
    members.stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(methodTree -> {
        var annotations = methodTree.modifiers().annotations();
        return annotations.isEmpty() || annotations.stream().noneMatch(EmptyMethodsCheck::isExceptedAnnotation);
      })
      .forEach(this::checkMethod);
  }

  /**
   * Returns true if the annotation indicates that the method body can legitimately be empty.
   */
  private static boolean isExceptedAnnotation(AnnotationTree annotationTree) {
    return annotationTree.symbolType().is(IGNORED_METHODS_ANNOTATION) ||
      (annotationTree.symbolType().isUnknown() && annotationTree.symbolType().name().equals(IGNORED_METHODS_ANNOTATION_UNQUALIFIED));
  }

  private void checkConstructors(List<Tree> members) {
    List<MethodTree> constructors = members.stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList());
    if (constructors.size() == 1 && isPublicNoArgConstructor(constructors.get(0))) {
      // In case that there is only a single public default constructor with empty body, we raise an issue, as this is equivalent to not
      // defining a constructor at all and hence redundant.
      checkMethod(constructors.get(0));
    } else if(constructors.size() > 1) {
      // If there are several constructors, it may be valid to have a no-args constructor with an empty body. However, constructors that
      // take arguments should do something with those or say why they don't using a comment.
      constructors.stream()
        .filter(constructor -> !constructor.parameters().isEmpty())
        .forEach(this::checkMethod);
    }
  }

  private static boolean isPublicNoArgConstructor(MethodTree constructor) {
    return ModifiersUtils.hasModifier(constructor.modifiers(), Modifier.PUBLIC) && constructor.parameters().isEmpty();
  }

  private void checkMethod(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null && isEmpty(block) && !containsComment(block)) {
      newIssue()
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
