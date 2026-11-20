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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.java.jdt.JDTParser;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class JParserProvidersTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.INFO);

  private static final JavaVersion VERSION = new JavaVersionImpl(17);

  @Test
  void default_frontend_is_used_without_providers() {
    JParser parser = JParserProviders.create(List.of(), JParser.Mode.BATCH, VERSION, List.of(), false, true);

    assertThat(parser).isInstanceOf(JDTParser.class);
  }

  @Test
  void default_frontend_is_used_when_no_provider_is_enabled() {
    StubProvider disabled = new StubProvider(false);

    JParser parser = JParserProviders.create(List.of(disabled), JParser.Mode.FILE_BY_FILE, VERSION, List.of(), false, true);

    assertThat(parser).isInstanceOf(JDTParser.class);
    assertThat(disabled.created).isZero();
  }

  @Test
  void first_enabled_provider_creates_the_parser_with_the_requested_arguments() {
    StubProvider disabled = new StubProvider(false);
    StubProvider enabled = new StubProvider(true);
    StubProvider later = new StubProvider(true);
    List<File> classpath = List.of(new File("a.jar"));

    JParser parser = JParserProviders.create(List.of(disabled, enabled, later), JParser.Mode.FILE_BY_FILE, VERSION, classpath, true, true);

    assertThat(parser).isSameAs(enabled.parser);
    assertThat(enabled.mode).isEqualTo(JParser.Mode.FILE_BY_FILE);
    assertThat(enabled.javaVersion).isSameAs(VERSION);
    assertThat(enabled.classpath).isEqualTo(classpath);
    assertThat(enabled.ignoreUnnamedModule).isTrue();
    assertThat(disabled.created).isZero();
    assertThat(later.created).isZero();
  }

  @Test
  void default_frontend_is_used_when_the_enabled_provider_cannot_create_its_parser() {
    StubProvider broken = new StubProvider(true);
    broken.failure = new IllegalStateException("no executable");

    JParser parser = JParserProviders.create(List.of(broken), JParser.Mode.BATCH, VERSION, List.of(), false, true);

    assertThat(parser).isInstanceOf(JDTParser.class);
    assertThat(logTester.logs(Level.WARN)).containsExactly("Frontend StubProvider is unavailable, falling back to the default one: no executable");
  }

  @Test
  void default_frontend_is_used_when_the_provider_cannot_tell_whether_it_is_enabled() {
    StubProvider broken = new StubProvider(true);
    broken.enabledFailure = new IllegalStateException("flag service down");

    JParser parser = JParserProviders.create(List.of(broken), JParser.Mode.BATCH, VERSION, List.of(), false, true);

    assertThat(parser).isInstanceOf(JDTParser.class);
  }

  @Test
  void every_provider_is_configured_even_when_one_fails() {
    StubProvider failing = new StubProvider(true);
    failing.failure = new IllegalArgumentException("bad settings");
    StubProvider healthy = new StubProvider(true);
    Configuration configuration = new MapSettings().asConfig();

    JParserProviders.configure(List.of(failing, healthy), configuration);

    assertThat(failing.configuration).isSameAs(configuration);
    assertThat(healthy.configuration).isSameAs(configuration);
    assertThat(logTester.logs(Level.WARN)).containsExactly("Frontend StubProvider could not be configured and is skipped: bad settings");
  }

  private static final class StubProvider implements JParserProvider {
    private final boolean enabled;
    private final JParser parser = (files, canceled, progress, action) -> {
    };
    private RuntimeException failure;
    private RuntimeException enabledFailure;
    private Configuration configuration;
    private int created;
    private JParser.Mode mode;
    private JavaVersion javaVersion;
    private List<File> classpath;
    private boolean ignoreUnnamedModule;

    private StubProvider(boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public void configure(Configuration configuration) {
      this.configuration = configuration;
      if (failure != null) {
        throw failure;
      }
    }

    @Override
    public boolean isEnabled() {
      if (enabledFailure != null) {
        throw enabledFailure;
      }
      return enabled;
    }

    @Override
    public JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
      boolean shouldUseJdkReleaseOption) {
      if (failure != null) {
        throw failure;
      }
      created++;
      this.mode = mode;
      this.javaVersion = javaVersion;
      this.classpath = classpath;
      this.ignoreUnnamedModule = shouldIgnoreUnnamedModuleForSplitPackage;
      return parser;
    }
  }
}
