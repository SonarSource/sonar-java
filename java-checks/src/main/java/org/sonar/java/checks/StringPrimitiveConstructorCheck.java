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

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2129")
public class StringPrimitiveConstructorCheck extends IssuableSubscriptionVisitor {

  private static final String QUICK_FIX_MESSAGE = "Replace this \"%s\" constructor with %s";
  private static final String REPLACEMENT_MESSAGE = "the %s literal passed as parameter";
  private static final String ISSUE_MESSAGE = "Remove this \"%s\" constructor";

  private static final String STRING = "java.lang.String";
  private static final BigInteger MIN_BIG_INTEGER_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger MAX_BIG_INTEGER_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

  private static final MethodMatchers EMPTY_STRING_MATCHER = MethodMatchers.create().ofTypes(STRING).constructor().addWithoutParametersMatcher().build();
  private static final MethodMatchers BIG_INT_MATCHER = primitiveConstructorMatcher("java.math.BigInteger", STRING);

  private static final MethodMatchers matchers = MethodMatchers.or(
    EMPTY_STRING_MATCHER,
    BIG_INT_MATCHER,
    primitiveConstructorMatcher(STRING, STRING),
    primitiveConstructorMatcher("java.lang.Byte", "byte"),
    primitiveConstructorMatcher("java.lang.Character", "char"),
    primitiveConstructorMatcher("java.lang.Short", "short"),
    primitiveConstructorMatcher("java.lang.Integer", "int"),
    primitiveConstructorMatcher("java.lang.Long", "long"),
    primitiveConstructorMatcher("java.lang.Float", "float"),
    primitiveConstructorMatcher("java.lang.Double", "double"),
    primitiveConstructorMatcher("java.lang.Boolean", "boolean"));

  private static MethodMatchers primitiveConstructorMatcher(String constructor, String param) {
    return MethodMatchers.create().ofTypes(constructor).constructor().addParametersMatcher(param).build();
  }

  private static final Map<String, String> classToLiteral = Map.of(
    "String", "string",
    "Double", "double",
    "Integer", "int",
    "Boolean", "boolean",
    "Byte", "byte",
    "Character", "char",
    "Short", "short",
    "Long", "long",
    "Float", "float"
    );


  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    if (newClassTree.classBody() != null) {
      return;
    }
    if (isBigIntegerPotentiallyBiggerThanLong(newClassTree)) {
      return;
    }
    if(matchers.matches(newClassTree)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(newClassTree.identifier())
        .withMessage(ISSUE_MESSAGE, newClassTree.symbolType().name())
        .withQuickFix(() -> computeQuickFix(newClassTree))
        .report();
    }
  }

  private static boolean isBigIntegerPotentiallyBiggerThanLong(NewClassTree newClassTree) {
    if (!newClassTree.symbolType().is("java.math.BigInteger")) {
      return false;
    }
    ExpressionTree argument = newClassTree.arguments().get(0);
    if (!argument.is(Tree.Kind.STRING_LITERAL)) {
      return true;
    }
    try {
      BigInteger value = new BigInteger(LiteralUtils.trimQuotes(((LiteralTree)argument).value()));
      return value.compareTo(MIN_BIG_INTEGER_VALUE) < 0 || value.compareTo(MAX_BIG_INTEGER_VALUE) > 0;
    } catch (NumberFormatException e) {
      return true;
    }
  }

  private JavaQuickFix computeQuickFix(NewClassTree newClassTree) {
    String message;
    JavaTextEdit textEdit;
    String className = newClassTree.symbolType().name();
    if (EMPTY_STRING_MATCHER.matches(newClassTree)) {
      message = formatQuickFixMessage(className, "an empty string \"\"");
      textEdit = JavaTextEdit.replaceTree(newClassTree, "\"\"");
    } else if (BIG_INT_MATCHER.matches(newClassTree)) {
      String arg = getFirstArgumentAsString(newClassTree).replace("\"", "") + "L";
      String replacement = String.format("BigInteger.valueOf(%s)", arg);
      message = formatQuickFixMessage(className, "\"BigInteger.valueOf()\" static method");
      textEdit = JavaTextEdit.replaceTree(newClassTree, replacement);
    } else {
      message = formatQuickFixMessage(className, String.format(REPLACEMENT_MESSAGE, classToLiteral.get(className)));
      String replacement = getFirstArgumentAsString(newClassTree);
      textEdit = JavaTextEdit.replaceTree(newClassTree, replacement);
    }
    return JavaQuickFix.newQuickFix(message).addTextEdit(textEdit).build();
  }

  private static String formatQuickFixMessage(String constructor, String replacement) {
    return String.format(QUICK_FIX_MESSAGE, constructor, replacement);
  }

  private String getFirstArgumentAsString(NewClassTree newClassTree) {
    ExpressionTree expr = newClassTree.arguments().get(0);
    return QuickFixHelper.contentForTree(expr, context);
  }

}
