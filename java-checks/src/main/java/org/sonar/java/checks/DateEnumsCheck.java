package org.sonar.java.checks;

import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

public class DateEnumsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final MethodMatchers LOCAL_DATE_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.LocalDate")
    .names("of")
    .addParametersMatcher("int", "int", "int")
    .build();

  private static final MethodMatchers LOCAL_DATE_TIME_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.LocalDateTime")
    .names("of")
    .addParametersMatcher("int", "int", "int", "int", "int")
    .addParametersMatcher("int", "int", "int", "int", "int", "int")
    .addParametersMatcher("int", "int", "int", "int", "int", "int", "int")
    .build();

  private static final MethodMatchers OFFSET_DATE_TIME_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.OffsetDateTime")
    .names("of")
    .addParametersMatcher("int", "int", "int", "int", "int", "int", "int", "java.time.ZoneOffset")
    .build();

  private static final MethodMatchers ZONED_DATE_TIME_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.ZonedDateTime")
    .names("of")
    .addParametersMatcher("int", "int", "int", "int", "int", "int", "int", "java.time.ZoneId")
    .build();

  private static final MethodMatchers YEAR_MONTH_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.YearMonth")
    .names("of")
    .addParametersMatcher("int", "int")
    .build();

  private static final MethodMatchers MONTH_DAY_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.MonthDay")
    .names("of")
    .addParametersMatcher("int", "int")
    .build();

  private static final MethodMatchers DAY_OF_WEEK_OF_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.DayOfWeek")
    .names("of")
    .addParametersMatcher("int")
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(LOCAL_DATE_TIME_OF_MATCHER, LOCAL_DATE_OF_MATCHER, OFFSET_DATE_TIME_OF_MATCHER, ZONED_DATE_TIME_OF_MATCHER, MONTH_DAY_OF_MATCHER,
      YEAR_MONTH_OF_MATCHER, DAY_OF_WEEK_OF_MATCHER);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit, "Use an enum instead of this numeric value");
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }
}
