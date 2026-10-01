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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;

/**
 * Stores the data collected per-module, per-file during Spring context collection.
 */
public class SpringContextGatheringModel {

  /**
   * The Spring data collected for a given file in a module.
   *
   * @param beans    The bean definitions gathered in the file.
   * @param packages The packages covered by the file.
   */
  public record InputFileData(@Nullable InputFile inputFile, List<BeanDefinitionHolder.InputFileData> beans, Set<String> packages) {
  }

  /**
   * The Spring data collected per-module, per-file.
   */
  private final Map<String, Map<String, InputFileData>> filesData = new HashMap<>();

  public void collectBeans(String moduleKey, String fileKey, @Nullable InputFile inputFile, List<BeanDefinitionHolder.InputFileData> beans) {
    filesData
      .computeIfAbsent(moduleKey, k -> new HashMap<>())
      .computeIfAbsent(fileKey, k -> new InputFileData(inputFile, new ArrayList<>(), new HashSet<>()))
      .beans.clear();
    filesData.get(moduleKey).get(fileKey).beans.addAll(beans);
  }

  public void collectPackages(String moduleKey, String fileKey, @Nullable InputFile inputFile, Set<String> packages) {
    filesData
      .computeIfAbsent(moduleKey, k -> new HashMap<>())
      .computeIfAbsent(fileKey, k -> new InputFileData(inputFile, new ArrayList<>(), new HashSet<>()))
      .packages.clear();
    filesData.get(moduleKey).get(fileKey).packages.addAll(packages);
  }

  public Collection<InputFileData> getInputFilesData(String moduleKey) {
    return filesData.getOrDefault(moduleKey, Map.of()).values();
  }

  public Set<String> getPackages(String moduleKey) {
    Set<String> packages = new HashSet<>();
    getInputFilesData(moduleKey).forEach(inputFileData -> packages.addAll(inputFileData.packages));
    return packages;
  }

}
