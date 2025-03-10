/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.cfg;

import org.junit.jupiter.api.Test;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CFGLoopTest {

  private static final CFGTestLoader loader = new CFGTestLoader("src/test/files/cfg/CFGLoopTest.java");

  private static List<CFG.Block> sorted(Collection<CFG.Block> collection) {
    List<CFG.Block> answer = new ArrayList<>(collection);
    Collections.sort(answer, (o1, o2) -> {
      // Use order of IDs
      return Integer.compare(o1.id(), o2.id());
    });
    return answer;
  }

  @Test
  void simpleWhileLoop() {
    String methodName = "simpleWhileLoop";
    final CFG cfg = buildCFG(methodName);
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    CFGLoop loop = loops.values().iterator().next();
    List<Block> blocks = sorted(cfg.blocks());
    assertThat(loop.startingBlock()).isSameAs(blocks.get(2));
    assertThat(loop.blocks()).containsOnly(blocks.get(1));
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  private CFG buildCFG(String methodName) {
    return (CFG) loader.getMethod("CFGLoopTest", methodName).cfg();
  }

  @Test
  void simpleWhileLoopWithBreak() {
    final CFG cfg = buildCFG("simpleWhileLoopWithBreak");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    CFGLoop loop = loops.values().iterator().next();
    List<Block> blocks = sorted(cfg.blocks());
    assertThat(loop.startingBlock()).isSameAs(blocks.get(3));
    assertThat(loop.blocks()).containsOnly(blocks.get(2), blocks.get(1));
    assertThat(loop.hasNoWayOut()).isFalse();
  }

  @Test
  void simpleForLoop() {
    final CFG cfg = buildCFG("simpleForLoop");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    CFGLoop loop = loops.values().iterator().next();
    List<Block> blocks = sorted(cfg.blocks());
    assertThat(loop.startingBlock()).isSameAs(blocks.get(3));
    assertThat(loop.blocks()).containsOnly(blocks.get(1), blocks.get(2));
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  @Test
  void embeddedMixedLoops() {
    final CFG cfg = buildCFG("embeddedMixedLoops");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(2);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.get(blocks.get(11).terminator());
    assertThat(loop.startingBlock()).isSameAs(blocks.get(11));
    assertThat(loop.blocks()).containsOnly(blocks.get(10), blocks.get(9), blocks.get(4), blocks.get(3), blocks.get(2), blocks.get(1));
    assertThat(loop.hasNoWayOut()).isTrue();
    loop = loops.get(blocks.get(9).terminator());
    assertThat(loop.startingBlock()).isSameAs(blocks.get(9));
    assertThat(loop.blocks()).containsOnly(blocks.get(8), blocks.get(7), blocks.get(6), blocks.get(5));
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  @Test
  void mixedWithForEach() {
    final CFG cfg = buildCFG("mixedWithForEach");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    // ForEach loops are not identified as loops!
    assertThat(loops).hasSize(1);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.startingBlock()).isSameAs(blocks.get(8));
    assertThat(loop.blocks()).containsOnly(blocks.get(7), blocks.get(6));
  }

  @Test
  void doWhile() {
    final CFG cfg = buildCFG("doWhile");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.startingBlock()).isSameAs(blocks.get(1));
    assertThat(loop.blocks()).containsOnly(blocks.get(2));
  }

  @Test
  void minimalForLoop() {
    final CFG cfg = buildCFG("minimalForLoop");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.startingBlock()).isSameAs(blocks.get(2));
    assertThat(loop.blocks()).containsOnly(blocks.get(1));
    assertThat(loop.hasNoWayOut()).isFalse();
  }

  @Test
  void emptyFor() {
    final CFG cfg = buildCFG("emptyFor");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.blocks()).isEmpty();
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  @Test
  void forWithOnlyInitializer() {
    final CFG cfg = buildCFG("forWithOnlyInitializer");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.blocks()).isEmpty();
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  @Test
  void emptyConditionFor() {
    final CFG cfg = buildCFG("emptyConditionFor");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.blocks()).containsOnly(blocks.get(1));
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  @Test
  void almostEmptyConditionFor() {
    final CFG cfg = buildCFG("almostEmptyConditionFor");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(1);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.values().iterator().next();
    assertThat(loop.blocks()).containsOnly(blocks.get(1));
    assertThat(loop.hasNoWayOut()).isTrue();
  }

  @Test
  void embeddedLoops() {
    final CFG cfg = buildCFG("embeddedLoops");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(2);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.get(blocks.get(6).terminator());
    assertThat(loop.blocks()).containsOnly(blocks.get(5), blocks.get(3));
    assertThat(loop.successors()).containsOnly(blocks.get(1), blocks.get(4));
    loop = loops.get(blocks.get(3).terminator());
    assertThat(loop.blocks()).containsOnly(blocks.get(2));
    assertThat(loop.successors()).containsOnly(blocks.get(1));
  }

  @Test
  void embeddedLoopsReturnInInnermost() {
    final CFG cfg = buildCFG("embeddedLoopsReturnInInnermost");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(2);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.get(blocks.get(5).terminator());
    assertThat(loop.blocks()).containsOnly(blocks.get(4), blocks.get(3));
    assertThat(loop.successors()).containsOnly(blocks.get(1));
    loop = loops.get(blocks.get(3).terminator());
    assertThat(loop.blocks()).containsOnly(blocks.get(2));
    assertThat(loop.successors()).containsOnly(blocks.get(1));
  }

  @Test
  void doubleReturnWhileLoop() {
    final CFG cfg = buildCFG("doubleReturnWhileLoop");
    Map<Tree, CFGLoop> loops = CFGLoop.getCFGLoops(cfg);
    assertThat(loops).hasSize(2);
    List<Block> blocks = sorted(cfg.blocks());
    CFGLoop loop = loops.get(blocks.get(6).terminator());
    assertThat(loop.blocks()).containsOnly(blocks.get(5), blocks.get(3));
    assertThat(loop.successors()).containsOnly(blocks.get(1), blocks.get(4));
    loop = loops.get(blocks.get(3).terminator());
    assertThat(loop.blocks()).containsOnly(blocks.get(2));
    assertThat(loop.successors()).containsOnly(blocks.get(1));
  }
}
