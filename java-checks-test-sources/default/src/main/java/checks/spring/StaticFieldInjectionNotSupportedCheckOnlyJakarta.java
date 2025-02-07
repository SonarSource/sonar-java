package checks.spring;

import jakarta.inject.Inject;

public class StaticFieldInjectionNotSupportedCheckOnlyJakarta {

  @Inject // no spring imports in the file, we consider we are not in a spring project
  public static String staticInject2;

  @javax.inject.Inject // no spring imports in the file, we consider we are not in a spring project
  private static Integer staticInject1;
}
