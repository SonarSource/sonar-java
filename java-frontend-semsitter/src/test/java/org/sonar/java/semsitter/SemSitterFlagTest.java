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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

class SemSitterFlagTest {

  @Test
  void property_name_is_the_contract_with_sonarqube_cloud() {
    assertThat(SemSitterFlag.ENABLED_PROPERTY).isEqualTo("sonar.java.internal.semsitter.enabled");
  }

  @Test
  void absent_property_keeps_the_default_frontend() {
    assertThat(SemSitterFlag.isEnabled(configuration(Map.of()))).isEqualTo(SemSitterFlag.DEFAULT).isFalse();
  }

  @Test
  void property_true_enables() {
    assertThat(SemSitterFlag.isEnabled(configuration(Map.of(SemSitterFlag.ENABLED_PROPERTY, "true")))).isTrue();
  }

  @Test
  void property_false_disables() {
    assertThat(SemSitterFlag.isEnabled(configuration(Map.of(SemSitterFlag.ENABLED_PROPERTY, "false")))).isFalse();
  }

  private static Configuration configuration(Map<String, String> properties) {
    MapSettings settings = new MapSettings();
    properties.forEach(settings::setProperty);
    return settings.asConfig();
  }
}
