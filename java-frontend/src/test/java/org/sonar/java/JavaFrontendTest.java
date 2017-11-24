/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java;

import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.java.JavaFrontend.ScannedFile;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaFrontendTest {

  private final JavaFrontend front = new JavaFrontend();

  @Test
  public void parse() {
    ScannedFile src = front.scan(new File("src/test/files/JavaFrontend.java"), this.getClass().getClassLoader());
    for (MethodTree m: getMethods(src.tree())) {
      System.out.println("visiting: " + m.simpleName());
      CFG cfg = CFG.build(m);
      simplifyCFG(src, cfg);
    }
  }

  private static Collection<MethodTree> getMethods(Tree tree) {
    List<MethodTree> result = new ArrayList<>();
    new BaseTreeVisitor() {
      {
        scan(tree);
      }

      @Override public void visitMethod(MethodTree methodTree) {
        super.visitMethod(methodTree);
        result.add(methodTree);
      }
    };
    return result;
  }

  private static void simplifyCFG(ScannedFile src, CFG cfg) {
    Set<Block> visited = new HashSet<>();
    Set<Block> worklist = new HashSet<>();

    Block entry = cfg.entry();
    worklist.add(entry);

    while (!worklist.isEmpty()) {
      Block block = worklist.iterator().next();
      worklist.remove(block);
      visited.add(block);

      System.out.println("  B" + block.id() + " -> " + Joiner.on(", ").join(block.successors().stream().map(b -> "B" + b.id()).collect(Collectors.toList())));
      for (Tree tree: block.elements()) {
        String fqtn;
        if (tree instanceof ExpressionTree) {
          ExpressionTree expr = (ExpressionTree) tree;
          fqtn = expr.symbolType().fullyQualifiedName();
        } else {
          fqtn = src.semantic().getSymbol(tree).type().fullyQualifiedName();
        }
        System.out.println(String.format("    %s: %s of type %s", tree.kind(), tree, fqtn));
      }

      for (Block successor: block.successors()) {
        if (!visited.contains(successor)) {
          worklist.add(successor);
        }
      }
    }
  }

}
