/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.java.jdt.JDTParser;
import org.sonar.plugins.java.api.JavaVersion;

/**
 * Chooses the frontend that parses the files: the first enabled {@link JParserProvider}, and the ECJ based frontend
 * when none is enabled or the chosen one cannot be created.
 */
public final class JParserProviders {

  private static final Logger LOG = LoggerFactory.getLogger(JParserProviders.class);

  private static final List<JParserProvider> DISCOVERED = discover();

  private JParserProviders() {
  }

  private static List<JParserProvider> discover() {
    List<JParserProvider> providers = new ArrayList<>();
    ServiceLoader.load(JParserProvider.class, JParserProviders.class.getClassLoader()).forEach(providers::add);
    return List.copyOf(providers);
  }

  public static void configure(Configuration configuration) {
    configure(DISCOVERED, configuration);
  }

  static void configure(List<JParserProvider> providers, Configuration configuration) {
    for (JParserProvider provider : providers) {
      try {
        provider.configure(configuration);
      } catch (RuntimeException e) {
        LOG.warn("Frontend {} could not be configured and is skipped: {}", provider.getClass().getSimpleName(), e.getMessage());
      }
    }
  }

  static JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
    boolean shouldUseJdkReleaseOption) {
    return create(DISCOVERED, mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
  }

  static JParser create(List<JParserProvider> providers, JParser.Mode mode, JavaVersion javaVersion, List<File> classpath,
    boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
    for (JParserProvider provider : providers) {
      try {
        if (provider.isEnabled()) {
          return provider.create(mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
        }
      } catch (RuntimeException e) {
        LOG.warn("Frontend {} is unavailable, falling back to the default one: {}", provider.getClass().getSimpleName(), e.getMessage());
      }
    }
    return JDTParser.create(mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
  }
}
