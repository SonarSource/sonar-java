/*
 * Copyright (C) 2009-2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.samples.java;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class JavaExtensionPlugin extends SonarPlugin {

  @Override
  public List getExtensions() {
    return ImmutableList.of(JavaExtensionRulesDefinition.class, JavaExtensionsCheckRegistrar.class, JavaExtensionsTestCheckRegistrar.class);
  }

}
