package org.sonar.samples.java;


import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

import java.util.Arrays;

public class JavaExtensionsCheckRegistrar implements CheckRegistrar {
  /**
   * Lists all the checks provided by the plugin
   */
  public static Class<? extends JavaCheck>[] checkClasses() {
    return new Class[]{ExampleCheck.class, SubscriptionExampleCheck.class};
  }

  @Override
  public void register(RegistrarContext registrarContext) {
    registrarContext.registerClassesForRepository(JavaExtensionRulesDefinition.REPOSITORY_KEY, Arrays.asList(checkClasses()));
  }

}

