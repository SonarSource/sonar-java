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

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1449")
public class StringMethodsWithLocaleCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final MethodMatchers STRING_FORMAT = MethodMatchers.create().ofTypes(STRING).names("format").withAnyParameters().build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes(STRING).names("toUpperCase", "toLowerCase").addWithoutParametersMatcher().build(),
      STRING_FORMAT
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree report = mit.methodSelect();
    if (STRING_FORMAT.matches(mit) && (isLocaleVariant(mit) || !usesLocaleDependentFormatteer(mit.arguments().get(0)))) {
      return;
    }
    if (report.is(Tree.Kind.MEMBER_SELECT)) {
      report = ((MemberSelectExpressionTree) report).identifier();
    }
    reportIssue(report, "Define the locale to be used in this String operation.");
  }

  private static boolean isLocaleVariant(MethodInvocationTree mit) {
    return mit.methodSymbol().parameterTypes().get(0).is("java.util.Locale");
  }

  private static boolean usesLocaleDependentFormatteer(ExpressionTree firstArg) {
    FormatterVisitor visitor = new FormatterVisitor();
    firstArg.accept(visitor);
    return visitor.hasLocaleDependantFormatter;
  }

  private static class FormatterVisitor extends BaseTreeVisitor {

    // source: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax
    private static final String ARGUMENT_INDEX = "(\\d+\\$)?";
    private static final String FLAGS = "([-,#+ 0(]+)?";
    private static final String FLAGS_WITH_FORCED_COMMA = "([-#+ 0(]*,[-#+ 0(]*)";
    private static final String WIDTH = "(\\d*\\.?\\d*)";
    private static final String INTEGER_LOCAL_SPECIFIC_FORMAT = FLAGS_WITH_FORCED_COMMA + WIDTH + "d";
    private static final String DATE_TIME_FLOATING_POINT_LOCAL_SPECIFIC_FORMAT = FLAGS + WIDTH + "[eEfgGaAtT]";

    private static final Pattern LOCALE_DEPENDENT_FORMATTERS = Pattern
      .compile("(.*)%" + ARGUMENT_INDEX + "(" + INTEGER_LOCAL_SPECIFIC_FORMAT + "|" + DATE_TIME_FLOATING_POINT_LOCAL_SPECIFIC_FORMAT + ")(.*)");

    private boolean hasLocaleDependantFormatter = false;

    @Override
    public void visitLiteral(LiteralTree tree) {
      String value = tree.value().replace("%%", "");
      if (tree.is(Tree.Kind.STRING_LITERAL) && LOCALE_DEPENDENT_FORMATTERS.matcher(value).matches()) {
        hasLocaleDependantFormatter = true;
      }
      super.visitLiteral(tree);
    }
  }
}
