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
package org.sonar.java.semsitter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.java.jdt.JDTParser;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserProvider;
import org.sonar.plugins.java.api.JavaVersion;

/**
 * Offers the SemSitter frontend when the analysis is allowed to use it. It is allowed when a SemSitter executable is
 * known and either {@value #ENABLED_PROPERTY} says so or, when that property is not set, the LaunchDarkly flag
 * {@value #FLAG_KEY} is on for the project. Anything that goes wrong while asking leaves the default frontend in use.
 */
public final class SemSitterParserProvider implements JParserProvider {

  public static final String FLAG_KEY = "sonar-java-semsitter-frontend";
  public static final String ENABLED_PROPERTY = "sonar.java.semsitter.enabled";
  public static final String EXECUTABLE_PROPERTY = "sonar.java.semsitter.executable";
  public static final String CLIENT_ID_PROPERTY = "sonar.java.semsitter.launchDarkly.clientSideId";
  public static final String EXECUTABLE_ENVIRONMENT = "SEMSITTER_ANALYZER";
  public static final String CLIENT_ID_ENVIRONMENT = "SONAR_JAVA_LAUNCHDARKLY_CLIENT_ID";
  static final boolean FLAG_DEFAULT = false;

  private static final Logger LOG = LoggerFactory.getLogger(SemSitterParserProvider.class);
  private static final String PROJECT_KEY_PROPERTY = "sonar.projectKey";
  private static final String UNKNOWN_PROJECT = "unknown";

  private final FeatureFlags.Connector flags;
  private final UnaryOperator<String> environment;
  @Nullable
  private final DocumentTranslator translator;
  private volatile boolean enabled;
  private volatile Path executable;

  public SemSitterParserProvider() {
    this(LaunchDarklyFeatureFlags::connect, System::getenv, SemSitterTranslator::translate);
  }

  SemSitterParserProvider(FeatureFlags.Connector flags, UnaryOperator<String> environment, @Nullable DocumentTranslator translator) {
    this.flags = flags;
    this.environment = environment;
    this.translator = translator;
  }

  @Override
  public void configure(Configuration configuration) {
    enabled = false;
    executable = null;
    Optional<String> path = firstNonBlank(configuration.get(EXECUTABLE_PROPERTY), Optional.ofNullable(environment.apply(EXECUTABLE_ENVIRONMENT)));
    if (path.isEmpty()) {
      return;
    }
    executable = Path.of(path.get());
    enabled = configuration.getBoolean(ENABLED_PROPERTY).orElseGet(() -> isFlagOn(configuration));
    if (enabled) {
      LOG.info("The Java analyzer will use the SemSitter frontend.");
    }
  }

  private boolean isFlagOn(Configuration configuration) {
    Optional<String> clientId = firstNonBlank(configuration.get(CLIENT_ID_PROPERTY), Optional.ofNullable(environment.apply(CLIENT_ID_ENVIRONMENT)));
    if (clientId.isEmpty()) {
      return FLAG_DEFAULT;
    }
    String project = configuration.get(PROJECT_KEY_PROPERTY).orElse(UNKNOWN_PROJECT);
    try {
      return flags.connect(clientId.get(), project).isEnabled(FLAG_KEY, FLAG_DEFAULT);
    } catch (RuntimeException e) {
      LOG.warn("The SemSitter frontend flag could not be read, the default frontend is used: {}", e.getMessage());
      return FLAG_DEFAULT;
    }
  }

  private static Optional<String> firstNonBlank(Optional<String> first, Optional<String> second) {
    return first.filter(value -> !value.isBlank()).or(() -> second.filter(value -> !value.isBlank()));
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
    Path server = executable;
    Supplier<JParser> defaultParser = () -> JDTParser.create(mode, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    if (translator == null) {
      return defaultParser.get();
    }
    return new SemSitterParser(mode, javaVersion, classpath, root -> SemSitterClient.start(server, root), defaultParser, translator);
  }
}
