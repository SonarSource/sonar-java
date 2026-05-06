package org.sonar.java.checks;

import java.util.List;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public class NowWithoutParametersCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers NOW = MethodMatchers.create()
    .ofTypes("java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime", "java.time.MonthDay", "java.time.OffsetDateTime",
      "java.time.OffsetTime", "java.time.Year", "java.time.YearMonth", "java.time.ZonedDateTime")
    .names("now")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof MethodInvocationTree mit && NOW.matches(mit) && mit.methodSelect() instanceof MemberSelectExpressionTree mset) {
      reportIssue(mset.identifier(), mit, "Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.");
    }
  }

}
