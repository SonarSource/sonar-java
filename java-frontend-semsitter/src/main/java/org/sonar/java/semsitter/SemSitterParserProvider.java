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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserProvider;
import org.sonar.plugins.java.api.JavaVersion;

/**
 * Offers the SemSitter frontend when the analysis is allowed to use it: a SemSitter executable is known and
 * {@link SemSitterFlag} is on, that is when SonarQube Cloud sent {@value SemSitterFlag#ENABLED_PROPERTY}. Once chosen,
 * the SemSitter frontend never hands files to another frontend.
 */
public final class SemSitterParserProvider implements JParserProvider {

  public static final String EXECUTABLE_PROPERTY = "sonar.java.semsitter.executable";
  public static final String EXECUTABLE_ENVIRONMENT = "SEMSITTER_ANALYZER";

  private static final Logger LOG = LoggerFactory.getLogger(SemSitterParserProvider.class);

  private final UnaryOperator<String> environment;
  private final DocumentTranslator translator;
  private volatile boolean enabled;
  private final AtomicReference<Path> executable = new AtomicReference<>();

  public SemSitterParserProvider() {
    this(System::getenv, SemSitterTranslator::translate);
  }

  SemSitterParserProvider(UnaryOperator<String> environment, DocumentTranslator translator) {
    this.environment = environment;
    this.translator = translator;
  }

  @Override
  public void configure(Configuration configuration) {
    enabled = false;
    executable.set(null);
    Optional<String> path = configuredExecutable(configuration);
    if (path.isEmpty()) {
      return;
    }
    executable.set(Path.of(path.get()));
    enabled = SemSitterFlag.isEnabled(configuration);
    if (enabled) {
      LOG.info("The Java analyzer will use the SemSitter frontend.");
    }
  }

  /** The non-blank {@value #EXECUTABLE_PROPERTY}, or else the non-blank environment variable {@value #EXECUTABLE_ENVIRONMENT}. */
  private Optional<String> configuredExecutable(Configuration configuration) {
    Optional<String> fromVariable = Optional.ofNullable(environment.apply(EXECUTABLE_ENVIRONMENT)).filter(value -> !value.isBlank());
    return configuration.get(EXECUTABLE_PROPERTY).filter(value -> !value.isBlank()).or(() -> fromVariable);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
    boolean shouldUseJdkReleaseOption) {
    Path server = executable.get();
    return new SemSitterParser(mode, javaVersion, classpath, root -> SemSitterClient.start(server, root), translator);
  }
}
