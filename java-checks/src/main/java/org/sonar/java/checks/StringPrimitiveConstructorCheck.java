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

import java.math.BigInteger;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2129")
public class StringPrimitiveConstructorCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final BigInteger MIN_BIG_INTEGER_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger MAX_BIG_INTEGER_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

  private static final MethodMatchers EMPTY_STRING_MATCHER = MethodMatchers.create().ofTypes(STRING).constructor().addWithoutParametersMatcher().build();
  private static final MethodMatchers PARAM_STRING_MATCHER = MethodMatchers.create().ofTypes(STRING).constructor().addParametersMatcher(STRING).build();
  private static final MethodMatchers DOUBLE_MATCHER = MethodMatchers.create().ofTypes("java.lang.Double").constructor().addParametersMatcher("double").build();
  private static final MethodMatchers INTEGER_MATCHER = MethodMatchers.create().ofTypes("java.lang.Integer").constructor().addParametersMatcher("int").build();
  private static final MethodMatchers BOOLEAN_MATCHER = MethodMatchers.create().ofTypes("java.lang.Boolean").constructor().addParametersMatcher("boolean").build();
  private static final MethodMatchers BYTE_MATCHER = MethodMatchers.create().ofTypes("java.lang.Byte").constructor().addParametersMatcher("byte").build();
  private static final MethodMatchers CHAR_MATCHER = MethodMatchers.create().ofTypes("java.lang.Character").constructor().addParametersMatcher("char").build();
  private static final MethodMatchers SHORT_MATCHER = MethodMatchers.create().ofTypes("java.lang.Short").constructor().addParametersMatcher("short").build();
  private static final MethodMatchers LONG_MATCHER = MethodMatchers.create().ofTypes("java.lang.Long").constructor().addParametersMatcher("long").build();
  private static final MethodMatchers FLOAT_MATCHER = MethodMatchers.create().ofTypes("java.lang.Float").constructor().addParametersMatcher("float").build();
  private static final MethodMatchers BIG_INT_MATCHER = MethodMatchers.create().ofTypes("java.math.BigInteger").constructor().addParametersMatcher(STRING).build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      EMPTY_STRING_MATCHER,
      PARAM_STRING_MATCHER,
      BYTE_MATCHER,
      CHAR_MATCHER,
      SHORT_MATCHER,
      INTEGER_MATCHER,
      LONG_MATCHER,
      FLOAT_MATCHER,
      DOUBLE_MATCHER,
      BOOLEAN_MATCHER,
      BIG_INT_MATCHER);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.classBody() != null) {
      return;
    }
    if(isBigIntegerPotentiallyBiggerThanLong(newClassTree)) {
      return;
    }
    reportWithQuickFix(newClassTree);
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

  private void reportWithQuickFix(NewClassTree newClassTree) {
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(newClassTree.identifier())
      .withMessage("Remove this \"" + newClassTree.symbolType().name() + "\" constructor")
      .withQuickFix(() -> computeQuickFix(newClassTree))
      .report();
  }

  private static JavaQuickFix computeQuickFix(NewClassTree newClassTree) {
    String message = "";
    JavaTextEdit textEdit = null;
    if (EMPTY_STRING_MATCHER.matches(newClassTree)) {
      message = "Replace this \"String\" constructor with an empty string \"\"";
      textEdit = JavaTextEdit.replaceTree(newClassTree, "\"\"");
    } else if (BIG_INT_MATCHER.matches(newClassTree)) {
      String arg = getFirstArgumentAsString(newClassTree).replace("\"", "");
      try {
        Integer.valueOf(arg);
      } catch (NumberFormatException e) {
        arg += "L";
      }
      String replacement = String.format("BigInteger.valueOf(%s)", arg);
      message = "Replace this \"BigInteger\" constructor with \"BigInteger.valueOf()\" static method";
      textEdit = JavaTextEdit.replaceTree(newClassTree, replacement);
    } else {
      Matchers matchers = Matchers.getByMatch(newClassTree);
      if (matchers != null) {
        message = String.format("Replace this \"%s\" constructor with the %s literal passed as parameter", matchers.constructor, matchers.literal);
        String replacement = getFirstArgumentAsString(newClassTree);
        textEdit = JavaTextEdit.replaceTree(newClassTree, replacement);
      }
    }
    return JavaQuickFix.newQuickFix(message).addTextEdit(textEdit).build();
  }

  private static String getFirstArgumentAsString(NewClassTree newClassTree) {
    ExpressionTree expr = newClassTree.arguments().get(0);
    if (expr instanceof LiteralTreeImpl) {
      LiteralTreeImpl arg = (LiteralTreeImpl) expr;
      return arg.value();
    } else if (expr instanceof IdentifierTreeImpl) {
      IdentifierTreeImpl arg = (IdentifierTreeImpl) expr;
      return arg.name();
    } else if (expr instanceof TypeCastExpressionTreeImpl){
      TypeCastExpressionTreeImpl arg = (TypeCastExpressionTreeImpl) expr;
      return arg.children().stream().map(Tree::firstToken).map(ft -> ft != null ? ft.text() : "").collect(Collectors.joining());
    }else {
      return "";
    }
  }

  private enum Matchers {
    PARAM_STRING(PARAM_STRING_MATCHER, "String", "string"),
    DOUBLE(DOUBLE_MATCHER, "Double", "double"),
    INTEGER(INTEGER_MATCHER, "Integer", "int"),
    BOOLEAN(BOOLEAN_MATCHER, "Boolean", "boolean"),
    BYTE(BYTE_MATCHER, "Byte", "byte"),
    CHAR(CHAR_MATCHER, "Character", "char"),
    SHORT(SHORT_MATCHER, "Short", "short"),
    LONG(LONG_MATCHER, "Long", "long"),
    FLOAT(FLOAT_MATCHER, "Float", "float");

    public final MethodMatchers matcher;
    public final String constructor;
    public final String literal;

    private Matchers(MethodMatchers matcher, String constructor, String literal) {
      this.matcher = matcher;
      this.constructor = constructor;
      this.literal = literal;
    }

    public static Matchers getByMatch(NewClassTree newClassTree) {
      for (Matchers matchers : values()) {
        if (matchers.matcher.matches(newClassTree)) {
          return matchers;
        }
      }
      return null;
    }
  }

}
