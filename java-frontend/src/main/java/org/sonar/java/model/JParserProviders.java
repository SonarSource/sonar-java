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
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import org.sonar.api.config.Configuration;
import org.sonar.java.jdt.JDTParser;
import org.sonar.plugins.java.api.JavaVersion;

/**
 * Chooses the frontend that parses the files: the first enabled {@link JParserProvider}, or the ECJ based frontend when
 * none is enabled. The frontends are independent: a provider that fails to load, to configure or to create its parser
 * fails the analysis instead of handing the files to another frontend.
 */
public final class JParserProviders {

  private static final AtomicReference<List<JParserProvider>> CONFIGURED = new AtomicReference<>(List.of());

  private JParserProviders() {
  }

  /** Loads and configures fresh providers for the current analysis. Nothing is loaded where no analysis configures them. */
  public static void configure(Configuration configuration) {
    List<JParserProvider> discovered = ServiceLoader.load(JParserProvider.class, JParserProviders.class.getClassLoader())
      .stream().map(ServiceLoader.Provider::get).toList();
    CONFIGURED.set(configure(discovered, configuration));
  }

  static List<JParserProvider> configure(List<JParserProvider> providers, Configuration configuration) {
    providers.forEach(provider -> provider.configure(configuration));
    return providers;
  }

  static JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
    boolean shouldUseJdkReleaseOption) {
    return create(CONFIGURED.get(), mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
  }

  static JParser create(List<JParserProvider> providers, JParser.Mode mode, JavaVersion javaVersion, List<File> classpath,
    boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
    for (JParserProvider provider : providers) {
      if (provider.isEnabled()) {
        return provider.create(mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
      }
    }
    return JDTParser.create(mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
  }
}
