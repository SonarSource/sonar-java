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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class PsiSyntaxCorpusTest {
  @ParameterizedTest(name = "{0}")
  @MethodSource("files")
  void repository_sources_preserve_tree_links_tokens_and_trivia(Path path) throws IOException {
    String source = Files.readString(path);
    var tree = PsiSyntaxTestSupport.parse(path.getFileName().toString(), source);
    PsiSyntaxTestSupport.assertStructure(tree, source);
  }

  static Stream<Path> files() throws IOException {
    try (var paths = Files.walk(Path.of("src"))) {
      return paths.filter(path -> path.startsWith(Path.of("src/main/java")) || path.startsWith(Path.of("src/test/java")))
        .filter(path -> path.toString().endsWith(".java")).sorted().toList().stream();
    }
  }

  @ParameterizedTest
  @MethodSource("controlFlow")
  void control_flow_preserves_branches_and_finally_blocks(String method, int blockCount, List<Tree.Kind> terminators, int edgeCount, int finallyCount) {
    var tree = PsiSyntaxTestSupport.parse("A.java", "class A {" + method + "}");
    var declaration = (MethodTree) ((ClassTree) tree.types().getFirst()).members().getFirst();
    var cfg = CFG.build(declaration);
    assertThat(cfg.blocks()).hasSize(blockCount);
    assertThat(cfg.blocks().stream().map(CFG.Block::terminator).filter(java.util.Objects::nonNull).map(Tree::kind))
      .containsExactlyInAnyOrderElementsOf(terminators);
    assertThat(cfg.blocks().stream().mapToInt(block -> block.successors().size()).sum()).isEqualTo(edgeCount);
    assertThat(cfg.blocks().stream().filter(CFG.Block::isFinallyBlock).count()).isEqualTo(finallyCount);
  }

  static Stream<Arguments> controlFlow() {
    return Stream.of(
      Arguments.of("void m(){ outer:for(int i=0;i<3;i++){if(i==1)continue outer;while(flag){if(flag)break;else return;}} }", 10, List.of(Tree.Kind.FOR_STATEMENT, Tree.Kind.IF_STATEMENT, Tree.Kind.CONTINUE_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.IF_STATEMENT, Tree.Kind.BREAK_STATEMENT, Tree.Kind.RETURN_STATEMENT), 13, 0),
      Arguments.of("int m(){return switch(x){case 1,2->3;case 4->{yield 5;}default->0;};}", 6, List.of(Tree.Kind.YIELD_STATEMENT, Tree.Kind.YIELD_STATEMENT, Tree.Kind.YIELD_STATEMENT, Tree.Kind.SWITCH_EXPRESSION, Tree.Kind.RETURN_STATEMENT), 7, 0),
      Arguments.of("void m(){switch(x){case 1:case 2:x++;break;default:while(flag){break;}}}", 6, List.of(Tree.Kind.BREAK_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.BREAK_STATEMENT, Tree.Kind.SWITCH_STATEMENT), 7, 0),
      Arguments.of("void m(){try{if(flag)return;throw new E();}finally{synchronized(this){x++;}}}", 8, List.of(Tree.Kind.IF_STATEMENT, Tree.Kind.RETURN_STATEMENT, Tree.Kind.THROW_STATEMENT, Tree.Kind.SYNCHRONIZED_STATEMENT), 8, 2),
      Arguments.of("void m(){do{if(flag)break;x++;}while(flag);assert flag:x;}", 6, List.of(Tree.Kind.IF_STATEMENT, Tree.Kind.BREAK_STATEMENT, Tree.Kind.DO_STATEMENT), 7, 0)
    );
  }
}
