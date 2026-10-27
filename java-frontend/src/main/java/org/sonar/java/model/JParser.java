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

import com.google.gson.JsonObject;
import com.sonar.sslr.api.RecognitionException;
import java.nio.file.Path;
import java.util.Objects;
import org.sonar.java.semsitter.SemsitterBridge;

public final class JParser {

  private JParser() {
  }

  /**
   * Parses one compilation unit without project-wide symbols. Batch analysis goes through
   * {@link JParserConfig}, which scans every file before translating so references resolve.
   */
  public static JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source) {
    Objects.requireNonNull(version);
    Objects.requireNonNull(unitName);
    Objects.requireNonNull(source);
    try (SemsitterBridge bridge = SemsitterBridge.start(bridgePath())) {
      JsonObject document = bridge.parse(source);
      JSema sema = new JSema();
      sema.ingestDocument(document);
      return SemsitterTranslator.translate(sema, document);
    } catch (RecognitionException syntaxError) {
      throw syntaxError;
    } catch (Exception failure) {
      throw new RecognitionException(1, "SemSitter: unable to parse " + unitName, failure);
    }
  }

  static Path bridgePath() {
    String configured = System.getProperty("sonar.java.semsitter.bridge");
    if (configured == null || configured.isBlank()) {
      configured = System.getenv("SEMSITTER_BRIDGE");
    }
    if (configured == null || configured.isBlank()) {
      throw new RecognitionException(1, "SemSitter bridge is not configured. Set sonar.java.semsitter.bridge or SEMSITTER_BRIDGE.");
    }
    return Path.of(configured);
  }

}
