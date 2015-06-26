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

  public static final String TEST_REPOSITORY_KEY = "java-extension-test";

  @Override
  public void define(final Context context) {
    final NewRepository repo = context.createRepository(REPOSITORY_KEY, "java");
    repo.setName(REPOSITORY_KEY);

    final NewRepository testRepo = context.createRepository(TEST_REPOSITORY_KEY, "java");
    testRepo.setName(TEST_REPOSITORY_KEY);

    // We could use a XML or JSON file to load all rule metadata, but
    // we prefer use annotations in order to have all information in a
    // single place
    final RulesDefinitionAnnotationLoader annotationLoader = new RulesDefinitionAnnotationLoader();
    annotationLoader.load(repo, JavaExtensionsCheckRegistrar.checkClasses());
    annotationLoader.load(testRepo, JavaExtensionsTestCheckRegistrar.checkClasses());
    repo.done();
    testRepo.done();
  }
}
