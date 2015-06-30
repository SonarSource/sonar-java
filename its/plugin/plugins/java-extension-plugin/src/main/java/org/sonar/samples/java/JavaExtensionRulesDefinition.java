/*
 * Copyright (C) 2009-2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.samples.java;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;

public class JavaExtensionRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "java-extension";

  @Override
  public void define(Context context) {
    NewRepository repo = context.createRepository(REPOSITORY_KEY, "java");
    repo.setName(REPOSITORY_KEY);

    // We could use a XML or JSON file to load all rule metadata, but
    // we prefer use annotations in order to have all information in a
    // single place
    RulesDefinitionAnnotationLoader annotationLoader = new RulesDefinitionAnnotationLoader();
    // Load custom check classes
    annotationLoader.load(repo, JavaExtensionsCheckRegistrar.checkClasses());
    // Load custom test check classes
    annotationLoader.load(repo, JavaExtensionsCheckRegistrar.testCheckClasses());
    repo.done();
  }
}
