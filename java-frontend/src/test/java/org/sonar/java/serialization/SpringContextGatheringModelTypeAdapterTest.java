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
package org.sonar.java.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.ProfileExpression;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringContextGatheringModelTypeAdapterTest {

  private static final Gson GSON = new Gson();
  private static final InputFile INPUT_FILE = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));

  @Test
  void round_trips_module_and_file_data_without_serializing_input_files() {
    var bean = new BeanDefinitionHolder.InputFileData("bean", "example.Bean", "example", new TextSpan(1, 0, 1, 4),
      false, ProfileExpression.UNCONDITIONAL, null, Map.of(), Set.of("example.Bean"));
    var model = new SpringContextGatheringModel();
    model.collectBeans("module-a", "file-a", INPUT_FILE, List.of(bean));
    model.collectPackages("module-a", "file-a", INPUT_FILE, Set.of("example"));
    model.collectBeans("module-a", "file-b", INPUT_FILE, List.of());
    model.collectPackages("module-b", "file-c", INPUT_FILE, Set.of("other"));

    String json = GSON.toJson(model);

    var filesData = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("filesData");
    assertThat(filesData.keySet()).containsExactlyInAnyOrder("module-a", "module-b");
    var file = filesData.getAsJsonObject("module-a").getAsJsonObject("file-a");
    assertThat(file.has("inputFile")).isFalse();
    assertThat(file.getAsJsonArray("beans")).hasSize(1);
    assertThat(file.getAsJsonArray("packages").get(0).getAsString()).isEqualTo("example");

    var restored = GSON.fromJson(json, SpringContextGatheringModel.class);
    assertThat(restored.filesData().get("module-a")).containsOnlyKeys("file-a", "file-b");
    assertThat(restored.filesData().get("module-b")).containsOnlyKeys("file-c");
    assertThat(restored.filesData().get("module-a").get("file-a").beans()).containsExactly(bean);
    assertThat(restored.filesData().get("module-a").get("file-a").packages()).containsExactly("example");
    assertThat(restored.filesData().get("module-a").get("file-b").beans()).isEmpty();
    assertThat(restored.filesData().get("module-b").get("file-c").packages()).containsExactly("other");
    restored.filesData().values().stream()
      .flatMap(files -> files.values().stream())
      .forEach(data -> assertThat(data.inputFile()).isNull());
  }

  @Test
  void ignores_input_file_property_when_reading_json() {
    String json = """
      {"filesData":{"module":{"file":{"inputFile":{"key":"ignored"},"beans":[],"packages":[]}}}}
      """;

    var restored = GSON.fromJson(json, SpringContextGatheringModel.class);

    assertThat(restored.filesData().get("module").get("file").inputFile()).isNull();
  }

  @Test
  void preserves_an_empty_module() {
    String json = "{\"filesData\":{\"module\":{}}}";

    var restored = GSON.fromJson(json, SpringContextGatheringModel.class);

    assertThat(restored.filesData().get("module")).isEmpty();
    assertThat(GSON.toJson(restored)).isEqualTo(json);
  }

  @Test
  void rejects_missing_file_data() {
    assertThatThrownBy(() -> GSON.fromJson("{}", SpringContextGatheringModel.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing JSON property 'filesData'");
  }

  @Test
  void rejects_missing_InputFileData_field() {
    String missingPackages = "{\"filesData\":{\"module\":{\"file\":{\"beans\":[]}}}}";
    assertThatThrownBy(() -> GSON.fromJson(missingPackages, SpringContextGatheringModel.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing JSON property 'packages'");
  }
}
