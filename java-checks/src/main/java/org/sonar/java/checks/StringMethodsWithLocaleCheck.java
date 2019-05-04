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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Rule(key = "S1449")
public class StringMethodsWithLocaleCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final MethodMatcher STRING_FORMAT = stringMethod().name("format").withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      stringMethod().name("toUpperCase").withoutParameter(),
      stringMethod().name("toLowerCase").withoutParameter(),
      STRING_FORMAT
    );
  }

  private static MethodMatcher stringMethod() {
    return MethodMatcher.create().typeDefinition(STRING);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree report = mit.methodSelect();
    if (STRING_FORMAT.matches(mit) && (isLocaleVariant(mit) || !usesLocaleDependentFormatteer(mit.arguments().get(0)))) {
      return;
    }
    if(report.is(Tree.Kind.MEMBER_SELECT)) {
      report = ((MemberSelectExpressionTree) report).identifier();
    }
    reportIssue(report, "Define the locale to be used in this String operation.");
  }

  private static boolean isLocaleVariant(MethodInvocationTree mit) {
    return ((Symbol.MethodSymbol) mit.symbol()).parameterTypes().get(0).is("java.util.Locale");
  }

  private static boolean usesLocaleDependentFormatteer(ExpressionTree firstArg) {
    FormatterVisitor visitor = new FormatterVisitor();
    firstArg.accept(visitor);
    return visitor.hasLocaleDependantFormatter;
  }

  private static class FormatterVisitor extends BaseTreeVisitor {

    // source: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax
    private static final String ARGUMENT_INDEX = "(\\d+\\$)?";
    private static final String FLAGS = "([-,#+ 0(]*)?";
    private static final String FLAGS_WITH_FORCED_COMMA = "([-#+ 0(]*,[-#+ 0(]*)";
    private static final String WIDTH = "([0-9]*\\.?[0-9]*)?";
    private static final String INTEGER_LOCAL_SPECIFIC_FORMAT = FLAGS_WITH_FORCED_COMMA + WIDTH + "d";
    private static final String DATE_TIME_FLOATING_POINT_LOCAL_SPECIFIC_FORMAT = FLAGS + WIDTH + "[eEfgGaAtT]";

    private static final Pattern LOCALE_DEPENDENT_FORMATTERS = Pattern
      .compile("(.*)%" + ARGUMENT_INDEX + "(" + INTEGER_LOCAL_SPECIFIC_FORMAT + "|" + DATE_TIME_FLOATING_POINT_LOCAL_SPECIFIC_FORMAT + ")(.*)");

    private boolean hasLocaleDependantFormatter = false;

    @Override
    public void visitLiteral(LiteralTree tree) {
      String value = tree.value().replaceAll("%%", "");
      if (tree.is(Tree.Kind.STRING_LITERAL) && LOCALE_DEPENDENT_FORMATTERS.matcher(value).matches()) {
        hasLocaleDependantFormatter = true;
      }
      super.visitLiteral(tree);
    }
  }
}
