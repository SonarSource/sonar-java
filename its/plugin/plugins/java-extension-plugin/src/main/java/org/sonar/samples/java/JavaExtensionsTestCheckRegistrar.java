package org.sonar.samples.java;

import java.util.Arrays;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

public class JavaExtensionsTestCheckRegistrar implements CheckRegistrar {

  /**
   * Lists all the checks provided by the plugin
   */
  public static Class<? extends JavaCheck>[] checkClasses() {
    return new Class[] {
      SubscriptionExampleTestCheck.class
    };
  }

  @Override
  public void register(final RegistrarContext registrarContext) {
    registrarContext.registerClassesForRepository(JavaExtensionRulesDefinition.REPOSITORY_KEY, Arrays.asList(checkClasses()));
  }

  @Override
  public Type type() {
    return Type.TEST_CHECKS;
  }

}
