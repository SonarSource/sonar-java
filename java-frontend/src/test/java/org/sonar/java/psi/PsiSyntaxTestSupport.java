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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

final class PsiSyntaxTestSupport {
  private PsiSyntaxTestSupport() {
  }

  static JavaTree.CompilationUnitTreeImpl parse(String unitName, String source) {
    var configuration = new JavaPsiConfiguration(null, List.of(), 26, true, true);
    try (var environment = JavaPsiEnvironment.open(configuration,
      List.of(new JavaPsiEnvironment.SourceSnapshot(Path.of(unitName), source, StandardCharsets.UTF_8)))) {
      return environment.read((project, files) -> new PsiParser(files.getFirst()).convert());
    }
  }

  static void assertStructure(JavaTree root, String source) {
    var nodes = nodes(root);
    for (var node : nodes) {
      Tree ancestor = node;
      while (ancestor.parent() != null) {
        ancestor = ancestor.parent();
      }
      assertThat(ancestor).as("root of %s", node.kind()).isSameAs(root);
      if (node.parent() != null) {
        assertThat(children(node.parent())).as("parent of %s", node.kind()).contains(node);
      }
    }
    int lineCount = source.split("\\r\\n?|\\n", -1).length;
    for (var node : nodes) {
      if (node instanceof SyntaxToken token) {
        assertRange(token.range(), lineCount);
        token.trivias().forEach(trivia -> assertRange(trivia.range(), lineCount));
      }
    }
  }

  static Set<Tree.Kind> kinds(JavaTree root) {
    var result = EnumSet.noneOf(Tree.Kind.class);
    nodes(root).forEach(tree -> {
      result.add(tree.kind());
      if (tree instanceof SyntaxToken token && !token.trivias().isEmpty()) {
        result.add(Tree.Kind.TRIVIA);
      }
    });
    return result;
  }

  private static void assertRange(Range range, int lineCount) {
    assertThat(range.start().line()).isBetween(1, lineCount);
    assertThat(range.end().line()).isBetween(range.start().line(), lineCount);
    assertThat(range.start().columnOffset()).isNotNegative();
    assertThat(range.end().columnOffset()).isNotNegative();
    if (range.start().line() == range.end().line()) {
      assertThat(range.end().columnOffset()).isGreaterThanOrEqualTo(range.start().columnOffset());
    }
  }

  private static List<Tree> nodes(Tree root) {
    List<Tree> result = new ArrayList<>();
    Set<Tree> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    collect(root, result, visited);
    return result;
  }

  private static void collect(Tree tree, List<Tree> result, Set<Tree> visited) {
    if (visited.add(tree)) {
      result.add(tree);
      children(tree).forEach(child -> collect(child, result, visited));
    }
  }

  private static List<Tree> children(Tree tree) {
    return tree.is(Tree.Kind.TOKEN, Tree.Kind.INFERED_TYPE) ? List.of() : ((JavaTree) tree).getChildren();
  }
}
