/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;


import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAgenticWayProfileTest {

  @Test
  void profile_is_registered_as_expected() {
    JavaAgenticWayProfile profile = new JavaAgenticWayProfile();
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);

    Map<String, Map<String, BuiltInQualityProfilesDefinition.BuiltInQualityProfile>> profilesPerLanguages = context.profilesByLanguageAndName();
    assertThat(profilesPerLanguages).containsOnlyKeys("java");
    assertThat(profilesPerLanguages.get("java")).containsOnlyKeys("AI Quality Profile");

    BuiltInQualityProfilesDefinition.BuiltInQualityProfile actualProfile = profilesPerLanguages.get("java").get("AI Quality Profile");
    assertThat(actualProfile.isDefault()).isFalse();
    assertThat(actualProfile.rules())
      .hasSize(468)
      .extracting(BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey)
      .doesNotContainAnyElementsOf(List.of(
        "S101",
        "S110",
        "S114",
        "S115",
        "S116",
        "S117",
        "S1066",
        "S1075",
        "S1104",
        "S1110",
        "S1124",
        "S1195",
        "S1197",
        "S1611",
        "S1659",
        "S1710",
        "S4719",
        "S4838",
        "S4925",
        "S4929",
        "S4968",
        "S4977",
        "S5261",
        "S5329",
        "S5411",
        "S5413",
        "S5663",
        "S5664",
        "S5665",
        "S5669",
        "S5786",
        "S5841",
        "S5853",
        "S5854",
        "S5857",
        "S5860",
        "S5869",
        "S5958",
        "S5961",
        "S5973",
        "S5976",
        "S5993",
        "S6019",
        "S6035",
        "S6213",
        "S6217",
        "S6219",
        "S6241",
        "S6242",
        "S6244",
        "S6246",
        "S6804",
        "S6813",
        "S6829",
        "S6830",
        "S6832",
        "S6833",
        "S6837",
        "S6912",
        "S8491"
      ));
  }
}
