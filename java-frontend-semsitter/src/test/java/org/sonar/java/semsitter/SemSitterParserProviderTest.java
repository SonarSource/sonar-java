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
import java.util.ArrayList;
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

  private final List<String> connections = new ArrayList<>();
  private boolean flagValue;
  private RuntimeException connectionFailure;
  private final DocumentTranslator translator = (document, types, packages, noLibraries, javaVersion) -> {
    throw new UnsupportedOperationException();
  };

  private final FeatureFlags.Connector connector = (clientId, projectKey) -> {
    if (connectionFailure != null) {
      throw connectionFailure;
    }
    connections.add(clientId + "@" + projectKey);
    return new FeatureFlags() {
      @Override
      public boolean isEnabled(String flagKey, boolean defaultValue) {
        return flagKey.equals(SemSitterParserProvider.FLAG_KEY) && flagValue;
      }
    };
  };

  @Test
  void provider_is_disabled_until_configured() {
    assertThat(provider(Map.of()).isEnabled()).isFalse();
  }

  @Test
  void provider_is_disabled_without_executable_and_never_asks_the_flag_service() {
    SemSitterParserProvider provider = provider(Map.of());
    flagValue = true;

    provider.configure(settings(Map.of(SemSitterParserProvider.CLIENT_ID_PROPERTY, "sdk")));

    assertThat(provider.isEnabled()).isFalse();
    assertThat(connections).isEmpty();
  }

  @Test
  void blank_executable_is_ignored() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "  ", SemSitterParserProvider.ENABLED_PROPERTY, "true")));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void explicit_property_enables_without_the_flag_service() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter", SemSitterParserProvider.ENABLED_PROPERTY, "true")));

    assertThat(provider.isEnabled()).isTrue();
    assertThat(connections).isEmpty();
  }

  @Test
  void explicit_property_disables_even_when_the_flag_is_on() {
    SemSitterParserProvider provider = provider(Map.of());
    flagValue = true;

    provider.configure(settings(Map.of(
      SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter",
      SemSitterParserProvider.ENABLED_PROPERTY, "false",
      SemSitterParserProvider.CLIENT_ID_PROPERTY, "sdk")));

    assertThat(provider.isEnabled()).isFalse();
    assertThat(connections).isEmpty();
  }

  @Test
  void executable_can_come_from_the_environment() {
    SemSitterParserProvider provider = provider(Map.of(SemSitterParserProvider.EXECUTABLE_ENVIRONMENT, "/bin/semsitter"));

    provider.configure(settings(Map.of(SemSitterParserProvider.ENABLED_PROPERTY, "true")));

    assertThat(provider.isEnabled()).isTrue();
  }

  @Test
  void without_property_nor_client_id_the_default_frontend_stays() {
    SemSitterParserProvider provider = provider(Map.of());

    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter")));

    assertThat(provider.isEnabled()).isEqualTo(SemSitterParserProvider.FLAG_DEFAULT);
    assertThat(connections).isEmpty();
  }

  @Test
  void flag_on_for_the_project_enables() {
    SemSitterParserProvider provider = provider(Map.of());
    flagValue = true;

    provider.configure(settings(Map.of(
      SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter",
      SemSitterParserProvider.CLIENT_ID_PROPERTY, "sdk",
      "sonar.projectKey", "my-project")));

    assertThat(provider.isEnabled()).isTrue();
    assertThat(connections).containsExactly("sdk@my-project");
  }

  @Test
  void flag_off_keeps_the_default_frontend() {
    SemSitterParserProvider provider = provider(Map.of());
    flagValue = false;

    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter", SemSitterParserProvider.CLIENT_ID_PROPERTY, "sdk")));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void client_id_can_come_from_the_environment_and_the_project_may_be_unknown() {
    SemSitterParserProvider provider = provider(Map.of(SemSitterParserProvider.CLIENT_ID_ENVIRONMENT, "env-sdk"));
    flagValue = true;

    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter")));

    assertThat(provider.isEnabled()).isTrue();
    assertThat(connections).containsExactly("env-sdk@unknown");
  }

  @Test
  void flag_service_failure_keeps_the_default_frontend() {
    SemSitterParserProvider provider = provider(Map.of());
    connectionFailure = new IllegalStateException("offline");

    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter", SemSitterParserProvider.CLIENT_ID_PROPERTY, "sdk")));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void configuring_again_forgets_the_previous_analysis() {
    SemSitterParserProvider provider = provider(Map.of());
    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, "/bin/semsitter", SemSitterParserProvider.ENABLED_PROPERTY, "true")));
    assertThat(provider.isEnabled()).isTrue();

    provider.configure(settings(Map.of()));

    assertThat(provider.isEnabled()).isFalse();
  }

  @Test
  void created_parser_is_the_semsitter_parser_once_a_translator_is_available() {
    SemSitterParserProvider provider = new SemSitterParserProvider(connector, Map.<String, String>of()::get, translator);
    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, Path.of("semsitter").toString(), SemSitterParserProvider.ENABLED_PROPERTY, "true")));

    JParser parser = provider.create(JParser.Mode.BATCH, new JavaVersionImpl(17), List.of(new File("a.jar")), false);

    assertThat(parser).isInstanceOf(SemSitterParser.class);
  }

  @Test
  void created_parser_is_the_default_frontend_without_a_translator() {
    SemSitterParserProvider provider = provider(Map.of());
    provider.configure(settings(Map.of(SemSitterParserProvider.EXECUTABLE_PROPERTY, Path.of("semsitter").toString(), SemSitterParserProvider.ENABLED_PROPERTY, "true")));

    JParser parser = provider.create(JParser.Mode.BATCH, new JavaVersionImpl(17), List.of(new File("a.jar")), false);

    assertThat(parser).isNotInstanceOf(SemSitterParser.class);
  }

  private SemSitterParserProvider provider(Map<String, String> environment) {
    UnaryOperator<String> lookup = environment::get;
    return new SemSitterParserProvider(connector, lookup, null);
  }

  private static Configuration settings(Map<String, String> properties) {
    MapSettings settings = new MapSettings();
    properties.forEach(settings::setProperty);
    return settings.asConfig();
  }
}
