/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.quickfixes.QuickFix;
import org.sonarsource.analyzer.commons.quickfixes.TextEdit;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1158")
public class ToStringUsingBoxingCheck extends IssuableSubscriptionVisitor {

  private static final String[] PRIMITIVE_WRAPPERS = new String[]{
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Boolean"
  };

  private static final MethodMatchers PRIMITIVE_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes(PRIMITIVE_WRAPPERS)
    .constructor()
    .addParametersMatcher(MethodMatchers.ANY)
    .build();
  private static final MethodMatchers PRIMITIVE_VALUE_OF = MethodMatchers.create()
    .ofTypes(PRIMITIVE_WRAPPERS)
    .names("valueOf")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();
  private static final MethodMatchers TO_STRING = MethodMatchers.create()
    // We are interested in any implementation of "toString", including the one from Integer
    .ofAnyType()
    .names("toString")
    .withAnyParameters()
    .build();
  private static final MethodMatchers COMPARE_TO = MethodMatchers.create()
    .ofSubTypes("java.lang.Comparable")
    .names("compareTo")
    .addParametersMatcher(MethodMatchers.ANY)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    ExpressionTree methodSelect = mit.methodSelect();
    if (!methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      return;
    }
    ExpressionTree memberSelectExpression = ((MemberSelectExpressionTree) methodSelect).expression();
    getArgumentOfPrimitiveWrapper(memberSelectExpression)
      .filter(arg -> arg.symbolType().isPrimitive())
      .ifPresent(arg -> reportIfCompareToOrToString(mit, memberSelectExpression, memberSelectExpression.symbolType().toString(), arg));
  }

  private static Optional<ExpressionTree> getArgumentOfPrimitiveWrapper(ExpressionTree memberSelectExpression) {
    if (memberSelectExpression.is(Tree.Kind.NEW_CLASS) && PRIMITIVE_CONSTRUCTOR.matches((NewClassTree) memberSelectExpression)) {
      return Optional.of(((NewClassTree) memberSelectExpression).arguments().get(0));
    } else if (memberSelectExpression.is(Tree.Kind.METHOD_INVOCATION) && PRIMITIVE_VALUE_OF.matches((MethodInvocationTree) memberSelectExpression)) {
      return Optional.of(((MethodInvocationTree) memberSelectExpression).arguments().get(0));
    }
    return Optional.empty();
  }

  private void reportIfCompareToOrToString(MethodInvocationTree mit, ExpressionTree memberSelectExpression, String boxedType, Tree argument) {
    Supplier<QuickFix> quickFix;
    String replacementMethod;
    if (TO_STRING.matches(mit)) {
      replacementMethod = "toString";
      if (mit.arguments().isEmpty()) {
        quickFix = toStringQuickFix(mit, boxedType, argument);
      } else {
        // The actual Integer.toString(...) is called, we want to keep the same arguments but change the first part
        quickFix = toStringWithArgumentQuickFix(memberSelectExpression, boxedType);
      }
    } else if (COMPARE_TO.matches(mit)) {
      replacementMethod = "compare";
      quickFix = compareToQuickFix(mit, boxedType, argument, mit.arguments().get(0));
    } else {
      return;
    }

    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(mit)
      .withMessage(String.format("Call the static method %s.%s(...) instead of instantiating a temporary object.", boxedType, replacementMethod))
      .withQuickFix(quickFix)
      .report();
  }

  private static Supplier<QuickFix> toStringQuickFix(MethodInvocationTree mit, String boxedType, Tree argument) {
    String replacement = String.format("%s.toString(", boxedType);
    return () ->
      QuickFix.newQuickFix(String.format("Use %s...) instead", replacement))
        .addTextEdit(
          TextEdit.replaceTextSpan(textSpanBetween(mit, true, argument, false), replacement),
          TextEdit.replaceTextSpan(textSpanBetween(argument, false, mit, true), ")")
        ).build();
  }


  private static Supplier<QuickFix> toStringWithArgumentQuickFix(ExpressionTree memberSelectExpression, String type) {
    return () ->
      QuickFix.newQuickFix(String.format("Use %s.toString(...) instead", type))
        .addTextEdit(
          AnalyzerMessage.replaceTree(memberSelectExpression, type)
        ).build();
  }

  private static Supplier<QuickFix> compareToQuickFix(MethodInvocationTree mit, String type, Tree firstArgument, Tree secondArgument) {
    String replacement = String.format("%s.compare(", type);
    return () ->
      QuickFix.newQuickFix(String.format("Use %s...) instead", replacement))
        .addTextEdit(
          TextEdit.replaceTextSpan(textSpanBetween(mit, true, firstArgument, false), replacement),
          TextEdit.replaceTextSpan(textSpanBetween(firstArgument, false, secondArgument, false), ", ")
        ).build();
  }

}
