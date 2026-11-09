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
package org.sonar.java.psi;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaVersion;

final class PsiSemanticTestSupport implements AutoCloseable {
  private final JavaPsiEnvironment environment;
  private final JavaTree.CompilationUnitTreeImpl tree;
  private final List<JavaTree.CompilationUnitTreeImpl> trees;

  private PsiSemanticTestSupport(JavaPsiEnvironment environment, List<JavaTree.CompilationUnitTreeImpl> trees) {
    this.environment = environment;
    this.trees = trees;
    this.tree = trees.getFirst();
  }

  static PsiSemanticTestSupport open(String name, String source, List<File> classpath, JavaVersion version) {
    return open(List.of(new JavaPsiEnvironment.SourceSnapshot(Path.of(name), source, StandardCharsets.UTF_8)), classpath, version);
  }

  static PsiSemanticTestSupport open(List<JavaPsiEnvironment.SourceSnapshot> sources, List<File> classpath, JavaVersion version) {
    var configuration = new JavaPsiConfiguration(null, classpath.stream().map(File::toPath).toList(),
      Integer.parseInt(version.effectiveJavaVersionAsString()), version.arePreviewFeaturesEnabled(), false);
    var environment = JavaPsiEnvironment.open(configuration, sources.stream()
      .map(source -> new JavaPsiEnvironment.SourceSnapshot(source.path(), PsiTokenIndex.unicodeInput(source.contents()).text(), source.charset())).toList());
    try {
      Runnable cleaner = environment::close;
      var trees = environment.read((project, files) -> {
        var result = new java.util.ArrayList<JavaTree.CompilationUnitTreeImpl>();
        for (int i = 0; i < files.size(); i++) {
          var tree = PsiParser.convert(environment, project, files.get(i), configuration, sources.get(i).contents(), cleaner);
          result.add(tree);
        }
        return List.copyOf(result);
      });
      return new PsiSemanticTestSupport(environment, trees);
    } catch (RuntimeException | Error failure) {
      try {
        environment.close();
      } catch (RuntimeException | Error cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
  }

  JavaTree.CompilationUnitTreeImpl tree() {
    return tree;
  }

  List<JavaTree.CompilationUnitTreeImpl> trees() {
    return trees;
  }

  @Override
  public void close() {
    environment.close();
  }
}
