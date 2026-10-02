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
package org.sonar.java.model.springcontext;

import com.google.gson.annotations.JsonAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.java.serialization.SpringContextGatheringModelTypeAdapter;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Stores the data collected per-module, per-file during Spring context collection.
 */
@ScannerSide
@SonarLintSide
@JsonAdapter(SpringContextGatheringModelTypeAdapter.class)
public class SpringContextGatheringModel {

  /**
   * The Spring data collected for a given file in a module.
   *
   * @param inputFile The source file, absent after JSON deserialization.
   * @param beans     The bean definitions gathered in the file.
   * @param packages  The packages covered by the file.
   */
  public record InputFileData(@Nullable InputFile inputFile, List<BeanDefinitionHolder.InputFileData> beans, Set<String> packages) {
  }

  /**
   * The Spring data collected per-module, per-file.
   */
  private final Map<String, Map<String, InputFileData>> filesData = new HashMap<>();
  private boolean restored;

  /**
   * Returns the file data indexed by module key and file key.
   *
   * @return An unmodifiable copy of the module map; the nested file maps and their data remain shared.
   */
  public Map<String, Map<String, InputFileData>> filesData() {
    return Map.copyOf(filesData);
  }

  /**
   * Returns whether data from a previous model has been restored.
   *
   * @return {@code true} after the first call to {@link #restoreFrom(SpringContextGatheringModel)}.
   */
  public boolean isRestored() {
    return restored;
  }

  /**
   * Restores file data from a previous model once, retaining data already collected for the same files.
   *
   * @param previous The model containing previously collected file data.
   */
  public void restoreFrom(SpringContextGatheringModel previous) {
    if (restored) {
      return;
    }
    previous.filesData.forEach((moduleKey, moduleData) -> {
      Map<String, InputFileData> current = filesData.computeIfAbsent(moduleKey, key -> new HashMap<>());
      moduleData.forEach(current::putIfAbsent);
    });
    restored = true;
  }

  /**
   * Registers a module even when it has no collected file data.
   *
   * @param moduleKey The module key.
   */
  public void ensureModule(String moduleKey) {
    filesData.computeIfAbsent(moduleKey, k -> new HashMap<>());
  }

  /**
   * Replaces the bean definitions collected for a file while retaining its collected packages.
   *
   * @param moduleKey The module key.
   * @param fileKey   The file key.
   * @param inputFile The source file, or {@code null} when unavailable.
   * @param beans     The bean definitions collected for the file.
   */
  public void collectBeans(String moduleKey, String fileKey, @Nullable InputFile inputFile, List<BeanDefinitionHolder.InputFileData> beans) {
    InputFileData data = inputFileData(moduleKey, fileKey, inputFile);
    data.beans.clear();
    data.beans.addAll(beans);
  }

  /**
   * Replaces the packages collected for a file while retaining its collected bean definitions.
   *
   * @param moduleKey The module key.
   * @param fileKey   The file key.
   * @param inputFile The source file, or {@code null} when unavailable.
   * @param packages  The packages collected for the file.
   */
  public void collectPackages(String moduleKey, String fileKey, @Nullable InputFile inputFile, Set<String> packages) {
    InputFileData data = inputFileData(moduleKey, fileKey, inputFile);
    data.packages.clear();
    data.packages.addAll(packages);
  }

  /**
   * Returns the collected file data for a module.
   *
   * @param moduleKey The module key.
   * @return The file data, or an empty collection if the module is absent.
   */
  public Collection<InputFileData> getInputFilesData(String moduleKey) {
    return filesData.getOrDefault(moduleKey, Map.of()).values();
  }

  /**
   * Returns all packages collected for files in a module.
   *
   * @param moduleKey The module key.
   * @return The collected packages, or an empty set if none were collected.
   */
  public Set<String> getPackages(String moduleKey) {
    Set<String> packages = new HashSet<>();
    getInputFilesData(moduleKey).forEach(inputFileData -> packages.addAll(inputFileData.packages));
    return packages;
  }

  private InputFileData inputFileData(String moduleKey, String fileKey, @Nullable InputFile inputFile) {
    Map<String, InputFileData> moduleData = filesData.computeIfAbsent(moduleKey, k -> new HashMap<>());
    InputFileData data = moduleData.computeIfAbsent(fileKey, k -> new InputFileData(inputFile, new ArrayList<>(), new HashSet<>()));
    if (inputFile != null && data.inputFile() != inputFile) {
      data = new InputFileData(inputFile, data.beans(), data.packages());
      moduleData.put(fileKey, data);
    }
    return data;
  }

}
