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

import com.sun.source.tree.CompilationUnitTree;
import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.LineColumnConverter;

public final class JavacBatchAnalysis implements AutoCloseable {

  private final JavacParser.Result parsed;
  private final Map<URI, JavaTree.CompilationUnitTreeImpl> trees;
  private final Map<URI, Exception> errors;

  private JavacBatchAnalysis(JavacParser.Result parsed, Map<URI, JavaTree.CompilationUnitTreeImpl> trees, Map<URI, Exception> errors) {
    this.parsed = parsed;
    this.trees = trees;
    this.errors = errors;
  }

  public static JavacBatchAnalysis analyze(Map<String, String> sources, JavacParser.Options options) throws IOException {
    JavacParser.Result parsed = JavacParser.parse(sources, options.withAnalysis(false));
    try {
      Map<URI, JavaTree.CompilationUnitTreeImpl> trees = new HashMap<>();
      Map<URI, Exception> errors = new HashMap<>();
      List<Mapping> mappings = new ArrayList<>();
      for (CompilationUnitTree unit : parsed.units()) {
        URI uri = unit.getSourceFile().toUri();
        Diagnostic<? extends JavaFileObject> syntaxError = parsed.diagnostics().stream()
          .filter(diagnostic -> JavacParser.isFatalSyntaxError(diagnostic)
            && diagnostic.getSource() != null && uri.equals(diagnostic.getSource().toUri()))
          .findFirst().orElse(null);
        if (syntaxError != null) {
          errors.put(uri, new RecognitionException((int) syntaxError.getLineNumber(),
            "Parse error at line " + syntaxError.getLineNumber() + " column " + syntaxError.getColumnNumber()
              + ": " + syntaxError.getMessage(null)));
          continue;
        }
        String source = sources.get(Path.of(uri).toString());
        try {
          JavaTree.CompilationUnitTreeImpl tree = JavacTreeConverter.convert(unit, parsed.trees(), source,
            (javacTree, sonarTree) -> mappings.add(new Mapping(unit, javacTree, sonarTree)));
          trees.put(uri, tree);
        } catch (RuntimeException e) {
          errors.put(uri, e);
        }
      }
      parsed.task().analyze();
      JavacSema sema = new JavacSema(parsed);
      for (Mapping mapping : mappings) {
        sema.register(mapping.unit, mapping.javacTree, mapping.sonarTree);
      }
      for (CompilationUnitTree unit : parsed.units()) {
        URI uri = unit.getSourceFile().toUri();
        JavaTree.CompilationUnitTreeImpl tree = trees.get(uri);
        if (tree == null) {
          continue;
        }
        tree.javacSema = sema;
        String source = sources.get(Path.of(uri).toString());
        var diagnostics = parsed.diagnostics().stream()
          .filter(diagnostic -> diagnostic.getSource() != null && uri.equals(diagnostic.getSource().toUri()))
          .toList();
        JWarning.Mapper.warningsFor(diagnostics, new LineColumnConverter(source))
          .withWarnings(JavacWarnings.unusedImports(unit, parsed.sourcePositions(), source, parsed.task().getElements()))
          .mappedInto(tree);
      }
      return new JavacBatchAnalysis(parsed, trees, errors);
    } catch (IOException | RuntimeException e) {
      parsed.close();
      throw e;
    }
  }

  public JavaTree.CompilationUnitTreeImpl tree(String path) {
    return trees.get(Path.of(path).toAbsolutePath().toUri());
  }

  public Exception error(String path) {
    return errors.get(Path.of(path).toAbsolutePath().toUri());
  }

  @Override
  public void close() throws IOException {
    parsed.close();
  }

  private record Mapping(CompilationUnitTree unit, com.sun.source.tree.Tree javacTree,
    org.sonar.plugins.java.api.tree.Tree sonarTree) {
  }
}
