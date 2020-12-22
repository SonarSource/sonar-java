package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5711")
public class DateTimeFormatterMismatch extends IssuableSubscriptionVisitor {
  private static final MethodMatchers OF_PATTERN_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatter")
    .names("ofPattern")
    .addParametersMatcher("java.lang.String")
    .addParametersMatcher("java.lang.String", "java.util.Locale")
    .build();

  private static final MethodMatchers APPEND_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatterBuilder")
    .names("appendValue")
    .addParametersMatcher("java.time.temporal.TemporalField", "int")
    .build();

  private static final MethodMatchers DATE_TIME_FORMATTER_BUILDER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatterBuilder")
    .names("toFormatter")
    .addParametersMatcher()
    .build();

  private static final Pattern WEEK_PATTERN = Pattern.compile(".*ww{1,2}.*");
  private static final Pattern YEAR_OF_ERA_PATTERN = Pattern.compile(".*[uy]+.*");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (OF_PATTERN_MATCHER.matches(invocation)) {
      Arguments arguments = invocation.arguments();
      ExpressionTree argument = arguments.get(0);
      if (argument.is(Tree.Kind.STRING_LITERAL)) {
        String pattern = ((LiteralTree) argument).value();
        if (WEEK_PATTERN.matcher(pattern).matches() && YEAR_OF_ERA_PATTERN.matcher(pattern).matches()) {
          reportIssue(invocation, "Change this year format to use the week-based year instead.");
        }
      }
    } else if (DATE_TIME_FORMATTER_BUILDER.matches(invocation)) {
      boolean usesWeekBasedYear = false;
      boolean usesWeekOfWeekBasedYear = false;
      Tree wanderer = invocation.methodSelect();
      while (wanderer != null && wanderer.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) wanderer;
        ExpressionTree expression = mset.expression();
        if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
          break;
        }
        MethodInvocationTree mit = (MethodInvocationTree) expression;
        if (APPEND_VALUE_MATCHER.matches(mit)) {
          if (!usesWeekBasedYear) {
            usesWeekBasedYear = isWeekBasedYearUsed(mit);
          }
          if (!usesWeekOfWeekBasedYear) {
            usesWeekOfWeekBasedYear = isWeekOfWeekBasedYearUsed(mit);
          }
        }
        wanderer = mit.methodSelect();
      }
      ExpressionTree lastExpression = ((MemberSelectExpressionTree) wanderer).expression();
      boolean conflictingWeekAndYear = usesWeekOfWeekBasedYear ^ usesWeekBasedYear;
      if (lastExpression.is(Tree.Kind.NEW_CLASS) && conflictingWeekAndYear) {
        reportIssue(invocation, "Change this year format to use the week-based year instead.");
      }
    }
  }

  private static boolean isWeekBasedYearUsed(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    ExpressionTree argument = arguments.get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree call = (MethodInvocationTree) argument;
      Symbol symbol = call.symbol();
      return symbol.name().equals("weekBasedYear");
    }
    return false;
  }

  private static boolean isWeekOfWeekBasedYearUsed(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    ExpressionTree argument = arguments.get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree call = (MethodInvocationTree) argument;
      Symbol symbol = call.symbol();
      return symbol.name().equals("weekOfWeekBasedYear");
    }
    return false;
  }
}
