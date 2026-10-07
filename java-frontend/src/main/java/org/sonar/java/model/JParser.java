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
import java.io.IOException;
import java.util.List;
import org.sonar.java.model.javac.JavacParser;
import org.sonar.java.model.javac.JavacSourceAnalysis;
import org.sonar.plugins.java.api.JavaVersion;

public final class JParser {

  private JParser() {
  }

  public static JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source, List<File> classpath) {
    return parse(JavaVersionImpl.fromString(version), unitName, source, classpath);
  }

  public static JavaTree.CompilationUnitTreeImpl parse(JavaVersion version, String unitName, String source, List<File> classpath) {
    JavacParser.Options options = JavacParser.Options.defaults()
      .withClasspath(classpath.stream().map(File::toPath).toList())
      .withSourceVersion(version.asInt() > 0 ? Math.max(8, version.asInt()) : Runtime.version().feature())
      .withPreview(version.arePreviewFeaturesEnabled() || version.asInt() <= 0);
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze(unitName, source, options)) {
      return analysis.tree();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to parse " + unitName, e);
    }
  }
}
