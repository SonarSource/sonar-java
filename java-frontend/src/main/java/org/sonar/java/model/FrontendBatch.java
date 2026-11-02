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
package org.sonar.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.fs.InputFile;

record FrontendBatch(List<InputFile> inputs, List<String> paths, List<String> encodings, Map<File, InputFile> byPath) {

  FrontendBatch {
    inputs = List.copyOf(inputs);
    paths = List.copyOf(paths);
    encodings = List.copyOf(encodings);
    byPath = Map.copyOf(byPath);
  }

  static FrontendBatch from(Iterable<? extends InputFile> inputFiles) {
    List<InputFile> inputs = new ArrayList<>();
    List<String> paths = new ArrayList<>();
    List<String> encodings = new ArrayList<>();
    Map<File, InputFile> byPath = new HashMap<>();
    for (InputFile input : inputFiles) {
      String path = input.absolutePath();
      byPath.put(new File(path), input);
      paths.add(path);
      encodings.add(input.charset().name());
      inputs.add(input);
    }
    return new FrontendBatch(inputs, paths, encodings, byPath);
  }
}
