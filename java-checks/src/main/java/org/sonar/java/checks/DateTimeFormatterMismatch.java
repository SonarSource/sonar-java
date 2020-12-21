package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
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
    }
  }
}
