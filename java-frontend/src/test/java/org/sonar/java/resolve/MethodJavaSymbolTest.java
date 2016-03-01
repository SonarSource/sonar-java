/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.fest.assertions.ObjectAssert;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class MethodJavaSymbolTest {

  @Test
  public void test() {
    File bytecodeDir = new File("target/test-classes");
    MethodVisitor methodVisitor = new MethodVisitor(Sets.newHashSet(28, 32, 40, 44, 46, 56, 72, 76, 84, 89, 91, 98, 100, 102), new HashSet<Integer>());
    JavaAstScanner.scanSingleFileForTests(
      new File("src/test/java/org/sonar/java/resolve/targets/MethodSymbols.java"),
      new VisitorsBridge(Collections.singleton(methodVisitor), Lists.newArrayList(bytecodeDir), null));
  }

  @Test
  public void test_unknowns() {
    MethodVisitor methodVisitor = new MethodVisitor(Sets.newHashSet(16, 21), Sets.newHashSet(7, 15, 17, 31));
    JavaAstScanner.scanSingleFileForTests(new File("src/test/files/resolve/MethodSymbols.java"), new VisitorsBridge(methodVisitor));
  }

  private static class MethodVisitor extends SubscriptionVisitor {
    private final Set<Integer> overrides;
    private final Set<Integer> unknowns;

    public MethodVisitor(Set<Integer> overrides, Set<Integer> unknowns) {
      this.overrides = overrides;
      this.unknowns = unknowns;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
      int line = ((JavaTree) tree).getLine();
      JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) ((MethodTree) tree).symbol();
      ObjectAssert assertion = assertThat(symbol.overriddenSymbol()).as("Method at line " + line);
      if (overrides.contains(line)) {
        assertion.isNotNull();
      } else if (unknowns.contains(line)) {
        assertion.isEqualTo(Symbols.unknownMethodSymbol);
      } else {
        assertion.isNull();
      }
    }
  }

}
