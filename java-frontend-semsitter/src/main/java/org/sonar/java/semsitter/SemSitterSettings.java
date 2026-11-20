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
package org.sonar.java.semsitter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.java.api.JavaVersion;

/**
 * The session settings SemSitter needs to analyze a project the way the ECJ based frontend does.
 */
final class SemSitterSettings {

  static final String LIBRARIES = "jvm.libraries";
  static final String JAVA_VERSION = "jvm.java_version";
  static final String ENABLE_PREVIEW = "jvm.enable_preview";

  private SemSitterSettings() {
  }

  /**
   * The libraries are the runtime image of {@code javaHome}, which is the JDK ECJ resolves against, followed by the
   * classpath, so that JDK classes win over a library copy as they do on the boot classpath. The language level and
   * the preview flag are only given when the project sets them.
   */
  static Map<String, List<String>> of(JavaVersion javaVersion, List<File> classpath, Path javaHome) {
    Map<String, List<String>> settings = new LinkedHashMap<>();
    List<String> libraries = new ArrayList<>();
    Path image = javaHome.resolve("lib").resolve("modules");
    if (Files.isRegularFile(image)) {
      libraries.add(image.toString());
    }
    classpath.stream().map(File::getAbsolutePath).forEach(libraries::add);
    settings.put(LIBRARIES, libraries);
    if (javaVersion.isSet()) {
      settings.put(JAVA_VERSION, List.of(String.valueOf(javaVersion.asInt())));
    }
    if (javaVersion.arePreviewFeaturesEnabled()) {
      settings.put(ENABLE_PREVIEW, List.of("true"));
    }
    return settings;
  }
}
