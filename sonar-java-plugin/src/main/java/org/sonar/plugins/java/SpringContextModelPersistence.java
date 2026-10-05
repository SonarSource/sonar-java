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
package org.sonar.plugins.java;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.java.model.springcontext.SpringContextGatheringModel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

final class SpringContextModelPersistence {

  private static final String DEFAULT_MODEL_PATH = ".sonar/spring-context-model.json";
  private static final Logger LOG = LoggerFactory.getLogger(SpringContextModelPersistence.class);
  private static final Gson GSON = new Gson();

  private SpringContextModelPersistence() {
  }

  static Path modelPath(SensorContext context, File rootDirectory) {
    String path = context.config().get(JavaSensor.SPRING_CONTEXT_MODEL_PATH_PROPERTY)
      .filter(value -> !value.isBlank())
      .orElse(DEFAULT_MODEL_PATH);
    return rootDirectory.toPath().resolve(path).toAbsolutePath().normalize();
  }

  static SpringContextGatheringModel load(Path path) {
    if (!Files.exists(path)) {
      return new SpringContextGatheringModel();
    }
    try (var reader = Files.newBufferedReader(path, UTF_8)) {
      SpringContextGatheringModel model = GSON.fromJson(reader, SpringContextGatheringModel.class);
      if (model == null) {
        throw new IllegalArgumentException("JSON document is empty");
      }
      return model;
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Unable to load Spring context model from " + path, e);
    }
  }

  static void save(Path path, SpringContextGatheringModel model) {
    try {
      Path directory = path.toAbsolutePath().getParent();
      Files.createDirectories(directory);
      Path temporaryFile = Files.createTempFile(directory, "spring-context-model-", ".tmp");
      try {
        try (var writer = Files.newBufferedWriter(temporaryFile, UTF_8)) {
          GSON.toJson(model, writer);
        }
        Files.move(temporaryFile, path, REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(temporaryFile);
      }
    } catch (IOException | JsonIOException e) {
      LOG.warn("Unable to save Spring context model to {}", path, e);
    }
  }
}
