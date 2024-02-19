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
    var profilePath = Path.of("src/main/resources/org/sonar/l10n/java/rules/java/Sonar_way_profile.json");
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
      .hasSameSizeAs(uniqueRuleKeys);

    var stringKeys = StreamSupport.stream(ruleKeys.spliterator(), false)
      .map(JsonElement::getAsString).toList();

    softly.assertThat(stringKeys)
      .doesNotHaveDuplicates()
      .containsExactlyElementsOf(sortedRuleKeys);

    softly.assertAll();
  }
}
