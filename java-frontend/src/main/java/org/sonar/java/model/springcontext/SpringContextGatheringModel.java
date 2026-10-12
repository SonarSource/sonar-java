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
 *
 * <p>This data structure is populated by {@link SpringContextModelGatherer} instances during the analysis.
 * A {@link SpringContextModel} instance is built from the structure's contents at the end of the analysis, to be
 * passed to {@code SpringContextCheck} instances to raise issues.</p>
 *
 * <p>The structure is serialized to JSON and saved to a file at the end of the analysis, so it can be restored and
 * reused in later analyses. Partial analyses retain restored data for unvisited files to provide whole-project context.
 * Full analyses remove data for unvisited files before building the context model, to reflect changes due to deleted
 * files.</p>
 */
@ScannerSide
@SonarLintSide
@JsonAdapter(SpringContextGatheringModelTypeAdapter.class)
public class SpringContextGatheringModel {

  public static final class InputFileData {
    @Nullable
    private InputFile inputFile;
    private final List<BeanDefinitionHolder.InputFileData> beans;
    private final Set<String> packages;

    private InputFileData(@Nullable InputFile inputFile) {
      this.inputFile = inputFile;
      this.beans = new ArrayList<>();
      this.packages = new HashSet<>();
    }

    @Nullable
    public InputFile inputFile() {
      return inputFile;
    }

    public List<BeanDefinitionHolder.InputFileData> beans() {
      return beans;
    }

    public Set<String> packages() {
      return packages;
    }
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
   * Removes data restored from a previous analysis for files that were not visited during the current analysis.
   */
  public void removeUnvisitedFiles() {
    filesData.values().forEach(moduleData -> moduleData.values().removeIf(data -> data.inputFile() == null));
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

  private InputFileData inputFileData(String moduleKey, String fileKey, @Nullable InputFile inputFile) {
    Map<String, InputFileData> moduleData = filesData.computeIfAbsent(moduleKey, k -> new HashMap<>());
    InputFileData data = moduleData.computeIfAbsent(fileKey, k -> new InputFileData(inputFile));
    if (inputFile != null) {
      data.inputFile = inputFile;
    }
    return data;
  }

}
