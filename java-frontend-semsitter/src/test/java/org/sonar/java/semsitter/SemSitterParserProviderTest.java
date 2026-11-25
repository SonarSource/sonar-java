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
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class SemSitterParserProviderTest {

  private static final String ENABLED = SemSitterFlag.ENABLED_PROPERTY;
  private static final String EXECUTABLE = SemSitterParserProvider.EXECUTABLE_PROPERTY;

  private final DocumentTranslator translator = (document, loaders, noLibraries, javaVersion) -> {
    throw new UnsupportedOperationException();
  };

  @Test
  void provider_is_disabled_until_configured() {
    assertThat(provider(Map.of()).isEnabled()).isFalse();
  }

  @Test
  void provider_is_disabled_without_executable() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(ENABLED, "true")));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void blank_executable_is_ignored() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(EXECUTABLE, "  ", ENABLED, "true")));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void property_enables_with_an_executable() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(EXECUTABLE, "/bin/semsitter", ENABLED, "true")));

    assertThat(provider.isEnabled()).isTrue();
  }

  @Test
  void property_false_keeps_the_default_frontend() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(EXECUTABLE, "/bin/semsitter", ENABLED, "false")));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void without_property_the_flag_default_applies() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(EXECUTABLE, "/bin/semsitter")));

    assertThat(provider.isEnabled()).isEqualTo(SemSitterFlag.DEFAULT);
  }

  @Test
  void executable_can_come_from_the_environment() {
    SemSitterParserProvider provider = provider(Map.of(SemSitterParserProvider.EXECUTABLE_ENVIRONMENT, "/bin/semsitter"));

    provider.configure(settings(Map.of(ENABLED, "true")));

    assertThat(provider.isEnabled()).isTrue();
  }

  @Test
  void configuring_again_forgets_the_previous_analysis() {
    SemSitterParserProvider provider = provider(Map.of());
    provider.configure(settings(Map.of(EXECUTABLE, "/bin/semsitter", ENABLED, "true")));
    assertThat(provider.isEnabled()).isTrue();

    provider.configure(settings(Map.of()));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void created_parser_is_the_semsitter_parser() {
    SemSitterParserProvider provider = provider(Map.of());
    provider.configure(settings(Map.of(EXECUTABLE, Path.of("semsitter").toString(), ENABLED, "true")));

    JParser parser = provider.create(JParser.Mode.BATCH, new JavaVersionImpl(17), List.of(new File("a.jar")), false, true);

    assertThat(parser).isInstanceOf(SemSitterParser.class);
  }

  private SemSitterParserProvider provider(Map<String, String> environment) {
    UnaryOperator<String> lookup = environment::get;
    return new SemSitterParserProvider(lookup, translator);
  }

  private static Configuration settings(Map<String, String> properties) {
    MapSettings settings = new MapSettings();
    properties.forEach(settings::setProperty);
    return settings.asConfig();
  }
}
