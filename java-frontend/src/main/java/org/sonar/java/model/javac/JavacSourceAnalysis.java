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
package org.sonar.java.model.javac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.sonar.sslr.api.RecognitionException;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.LineColumnConverter;

public final class JavacSourceAnalysis implements AutoCloseable {

  private final JavacParser.Result parsed;
  private final JavaTree.CompilationUnitTreeImpl tree;
  private final JavacSema sema;

  private JavacSourceAnalysis(JavacParser.Result parsed, JavaTree.CompilationUnitTreeImpl tree, JavacSema sema) {
    this.parsed = parsed;
    this.tree = tree;
    this.sema = sema;
  }

  public static JavacSourceAnalysis analyze(String fileName, String source, JavacParser.Options options) throws IOException {
    JavacParser.Result parsed = JavacParser.parse(fileName, source, options.withAnalysis(false));
    try {
      for (Diagnostic<? extends JavaFileObject> diagnostic : parsed.diagnostics()) {
        if (JavacParser.isFatalSyntaxError(diagnostic)) {
          throw new RecognitionException((int) diagnostic.getLineNumber(),
            "Parse error at line " + diagnostic.getLineNumber() + " column " + diagnostic.getColumnNumber() + ": " + diagnostic.getMessage(null));
        }
      }
      List<Map.Entry<org.sonar.java.javac.source.tree.Tree, org.sonar.plugins.java.api.tree.Tree>> mappings = new ArrayList<>();
      JavaTree.CompilationUnitTreeImpl tree = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source, options.sourceVersion(),
        (javacTree, sonarTree) -> mappings.add(Map.entry(javacTree, sonarTree)));
      parsed.task().analyze();
      JavacSema sema = new JavacSema(parsed);
      mappings.forEach(mapping -> sema.register(parsed.unit(), mapping.getKey(), mapping.getValue()));
      tree.javacSema = sema;
      JWarning.Mapper.warningsFor(parsed.diagnostics(), new LineColumnConverter(source))
        .withWarnings(JavacWarnings.unusedImports(parsed.unit(), parsed.sourcePositions(), source, parsed.task().getElements()))
        .withWarnings(JavacWarnings.unreachableCatches(parsed.unit(), parsed.sourcePositions(), source, parsed.diagnostics()))
        .mappedInto(tree);
      return new JavacSourceAnalysis(parsed, tree, sema);
    } catch (IOException | RuntimeException e) {
      parsed.close();
      throw e;
    }
  }

  public JavaTree.CompilationUnitTreeImpl tree() {
    return tree;
  }

  public JavacSema sema() {
    return sema;
  }

  public List<Diagnostic<? extends JavaFileObject>> diagnostics() {
    return parsed.diagnostics();
  }

  @Override
  public void close() throws IOException {
    parsed.close();
  }
}
