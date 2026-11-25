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
import org.sonar.api.config.Configuration;
import org.sonar.java.jdt.JDTParser;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JParserProvidersTest {

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
  void an_enabled_provider_that_cannot_create_its_parser_fails_instead_of_falling_back() {
    StubProvider broken = new StubProvider(true);
    broken.failure = new IllegalStateException("no executable");
    List<JParserProvider> providers = List.of(broken);

    assertThatThrownBy(() -> JParserProviders.create(providers, JParser.Mode.BATCH, VERSION, List.of(), false, true))
      .isInstanceOf(IllegalStateException.class).hasMessage("no executable");
  }

  @Test
  void a_provider_that_cannot_tell_whether_it_is_enabled_fails_instead_of_falling_back() {
    StubProvider broken = new StubProvider(true);
    broken.enabledFailure = new IllegalStateException("flag service down");
    List<JParserProvider> providers = List.of(broken);

    assertThatThrownBy(() -> JParserProviders.create(providers, JParser.Mode.BATCH, VERSION, List.of(), false, true))
      .hasMessage("flag service down");
  }

  @Test
  void every_provider_is_configured() {
    StubProvider first = new StubProvider(true);
    StubProvider second = new StubProvider(false);
    Configuration configuration = new MapSettings().asConfig();

    assertThat(JParserProviders.configure(List.of(first, second), configuration)).containsExactly(first, second);
    assertThat(first.configuration).isSameAs(configuration);
    assertThat(second.configuration).isSameAs(configuration);
  }

  @Test
  void a_provider_that_cannot_be_configured_fails_the_analysis() {
    StubProvider failing = new StubProvider(true);
    failing.failure = new IllegalArgumentException("bad settings");
    List<JParserProvider> providers = List.of(failing);
    Configuration configuration = new MapSettings().asConfig();

    assertThatThrownBy(() -> JParserProviders.configure(providers, configuration)).hasMessage("bad settings");
  }

  @Test
  void default_frontend_parses_when_no_installed_provider_accepted_the_configuration() {
    JParserProviders.configure(new MapSettings().asConfig());

    assertThat(JParserProviders.create(JParser.Mode.BATCH, VERSION, List.of(), false, true)).isInstanceOf(JDTParser.class);
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
