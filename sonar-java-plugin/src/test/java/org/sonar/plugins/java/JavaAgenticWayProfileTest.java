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


import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAgenticWayProfileTest {

  private static final Path RULE_DESCRIPTION_DIRECTORY = Path.of("src", "main", "resources", "org", "sonar", "l10n", "java", "rules", "java");

  @Test
  void profile_is_registered_as_expected() {
    JavaAgenticWayProfile profile = new JavaAgenticWayProfile();
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);

    Map<String, Map<String, BuiltInQualityProfilesDefinition.BuiltInQualityProfile>> profilesPerLanguages = context.profilesByLanguageAndName();
    assertThat(profilesPerLanguages).containsOnlyKeys("java");
    assertThat(profilesPerLanguages.get("java")).containsOnlyKeys("Sonar agentic AI");

    BuiltInQualityProfilesDefinition.BuiltInQualityProfile actualProfile = profilesPerLanguages.get("java").get("Sonar agentic AI");
    assertThat(actualProfile.isDefault()).isFalse();
    assertThat(actualProfile.rules())
      .hasSize(467)
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


  @Test
  @Disabled("A utility method to generate a quality profile from a CSV file")
  void generate_ai_quality_profile() throws IOException {
    Path generatedQualityProfile = generate(
      Path.of("Path to your CSV input file relative to sonar-java-plugin"),
      RULE_DESCRIPTION_DIRECTORY.resolve("Sonar_agentic_ai_profile.json"),
      JavaAgenticWayProfile.PROFILE_NAME
    );
    Assertions.fail(String.format("The generated quality profile was written to %s".formatted(generatedQualityProfile)));
  }

  static Path generate(Path input, Path output, String profileName) throws IOException {
    CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
      .setHeader()
      .setSkipHeaderRecord(true)
      .get();

    Set<String> keysOfImplementedRules = getImplementedRuleKeys();

    String ruleKeys;
    try (FileReader in = new FileReader(input.toFile(), StandardCharsets.UTF_8)) {
      ruleKeys = csvFormat.parse(in).stream()
        // Filter rules that have not been classified as AI and Sonar Way
        .filter(ruleRecord -> "AI and Sonar way".equalsIgnoreCase(ruleRecord.get("classification_status")))
        // Recover keys
        .map(ruleRecord -> ruleRecord.get("ruleid"))
        // Filter out keys that do not have a sonar-java implementation
        .filter(keysOfImplementedRules::contains)
        // Sort keys
        .sorted(JavaAgenticWayProfileTest::compareRuleKeys)
        // Surround with double quotes for output in JSON document
        .map("    \"%s\""::formatted)
        // Comma and line separate rule keys
        .collect(Collectors.joining(",%s".formatted(System.lineSeparator())));
    }
    try (
      FileWriter out = new FileWriter(output.toFile(), StandardCharsets.UTF_8);
      BufferedWriter writer = new BufferedWriter(out)
    ) {
      writer.write("{");
      writer.newLine();
      writer.write("  \"name\": \"%s\",".formatted(profileName));
      writer.newLine();
      writer.write("  \"ruleKeys\": [");
      writer.newLine();
      writer.write(ruleKeys);
      writer.newLine();
      writer.write("  ]");
      writer.newLine();
      writer.write("}");
    }
    return output;

  }

  private static Set<String> getImplementedRuleKeys() throws IOException {
    if (!Files.isDirectory(RULE_DESCRIPTION_DIRECTORY)) {
      throw new IllegalStateException("Could not find path to %s".formatted(RULE_DESCRIPTION_DIRECTORY));
    }
    return Files.list(RULE_DESCRIPTION_DIRECTORY)
      .filter(path -> !path.toString().endsWith("_profile.json"))
      .map(path -> {
        String fileName = path.getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
      })
      .collect(Collectors.toSet());
  }

  private static Integer getSortingKey(String ruleKey) {
    try {
      return Integer.parseInt(ruleKey.substring(1));
    } catch (NumberFormatException ignored) {
      return Integer.MIN_VALUE;
    }
  }

  private static int compareRuleKeys(String first, String second) {
    return getSortingKey(first) - getSortingKey(second);
  }
}
