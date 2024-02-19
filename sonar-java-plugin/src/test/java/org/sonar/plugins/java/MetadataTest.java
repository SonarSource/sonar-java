/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.java;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public class MetadataTest {
  @Test
  void ensure_sane_Sonar_way_profile() throws FileNotFoundException {
    var profilePath = Path.of("src/main/resources/" + JavaSonarWayProfile.SONAR_WAY_PATH);
    var reader = Files.newReader(profilePath.toFile(), Charsets.UTF_8);

    Gson gson = new Gson();
    var json = gson.fromJson(reader, JsonObject.class);

    var jsonKeys = json.keySet();

    var softly = new SoftAssertions();
    softly.assertThat(jsonKeys).containsExactlyInAnyOrder("name", "ruleKeys");

    var profileName = json.getAsJsonPrimitive("name").getAsString();
    softly.assertThat(profileName).isEqualTo("Sonar way");

    var ruleKeys = json.getAsJsonArray("ruleKeys");
    Set<String> uniqueRuleKeys = StreamSupport.stream(ruleKeys.spliterator(), false)
      .map(JsonElement::getAsString).collect(Collectors.toSet());
    List<String> sortedRuleKeys = uniqueRuleKeys.stream()
      .sorted(Comparator.comparingInt(key -> Integer.parseInt(key.substring(1))))
      .toList();

    softly.assertThat(ruleKeys)
      .hasSameSizeAs(uniqueRuleKeys)
      // Sanity check for a reasonable number of rules in the quality profile:
      .hasSizeGreaterThan(400);

    var stringKeys = StreamSupport.stream(ruleKeys.spliterator(), false)
      .map(JsonElement::getAsString).toList();

    softly.assertThat(stringKeys)
      .doesNotHaveDuplicates()
      .containsExactlyElementsOf(sortedRuleKeys);

    softly.assertAll();
  }
}
