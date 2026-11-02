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
import java.util.List;
import org.sonar.plugins.java.api.JavaVersion;

public final class FrontendParser {

  private FrontendParser() {
  }

  public static ParsedFile parse(JavaVersion version, List<File> classpath, String unitName, String source) {
    JParserConfig config = JParserConfig.Mode.FILE_BY_FILE.create(version, classpath);
    return parse(config.backend(), config.configuration, unitName, source);
  }

  /** Closes the session after parsing exceptions or errors, preserving the parsing failure and suppressing cleanup failures. */
  static ParsedFile parse(FrontendBackend backend, FrontendConfiguration configuration, String unitName, String source) {
    FrontendSession session = backend.open(configuration);
    try {
      return new ParsedFile(session.parse(unitName, source), session);
    } catch (RuntimeException | Error failure) {
      try {
        session.close();
      } catch (RuntimeException | Error cleanupFailure) {
        if (failure != cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      throw failure;
    }
  }

  public static final class ParsedFile implements AutoCloseable {
    private final JavaTree.CompilationUnitTreeImpl tree;
    private final FrontendSession session;

    private ParsedFile(JavaTree.CompilationUnitTreeImpl tree, FrontendSession session) {
      this.tree = tree;
      this.session = session;
    }

    public JavaTree.CompilationUnitTreeImpl tree() {
      return tree;
    }

    @Override
    public void close() {
      session.close();
    }
  }
}
