/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodJavaSymbolTest {

  @Test
  public void test() {
    File bytecodeDir = new File("target/test-classes");
    MethodVisitor methodVisitor = new MethodVisitor(Sets.newHashSet(28, 32, 40, 44, 46, 56, 72, 76, 84, 89, 91, 98, 100, 102), new HashSet<Integer>());
    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/MethodSymbols.java"),
      new VisitorsBridge(Collections.singleton(methodVisitor), Lists.newArrayList(bytecodeDir), null));
  }

  @Test
  public void test_unknowns() {
    MethodVisitor methodVisitor = new MethodVisitor(Sets.newHashSet(16, 21), Sets.newHashSet(7, 15, 17, 31));
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/files/resolve/MethodSymbols.java"), new VisitorsBridge(methodVisitor));
  }

  @Test
  public void test_throws() {
    File bytecodeDir = new File("target/test-classes");
    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/MethodThrowingExceptionsUsage.java"),
      new VisitorsBridge(Collections.singleton(new SubscriptionVisitor() {
        @Override
        public List<Tree.Kind> nodesToVisit() {
          return Lists.newArrayList(Tree.Kind.METHOD_INVOCATION);
        }

        @Override
        public void visitNode(Tree tree) {
          Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) ((MethodInvocationTree) tree).symbol();
          List<Type> thrownTypes = methodSymbol.thrownTypes();
          assertThat(thrownTypes).hasSize(2);
          if("test".equals(methodSymbol.name())) {
            assertThat(((JavaType) thrownTypes.get(0)).isTagged(JavaType.TYPEVAR)).isTrue(); // FIXME substitution should be done : see SONARJAVA-1778
          } else {
            assertThat(thrownTypes.get(0).is("java.sql.SQLException")).isTrue();
          }
          assertThat(thrownTypes.get(1).is("java.io.IOException")).isTrue();
        }
      }), Lists.newArrayList(bytecodeDir), null));
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
      return Collections.singletonList(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
      int line = ((JavaTree) tree).getLine();
      JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) ((MethodTree) tree).symbol();
      AbstractObjectAssert<?, JavaSymbol.MethodJavaSymbol> assertion = assertThat(symbol.overriddenSymbol()).as("Method at line " + line);
      if (overrides.contains(line)) {
        assertion.isNotNull();
      } else if (unknowns.contains(line)) {
        assertion.isEqualTo(Symbols.unknownMethodSymbol);
      } else {
        assertion.isNull();
      }
    }
  }

  @Test
  public void test_signature() throws Exception {
    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/MethodCompleteSignature.java"),
      new VisitorsBridge(Collections.singleton(new SubscriptionVisitor() {
        @Override
        public List<Tree.Kind> nodesToVisit() {
          return Lists.newArrayList(Tree.Kind.METHOD_INVOCATION);
        }

        @Override
        public void visitNode(Tree tree) {
          Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) ((MethodInvocationTree) tree).symbol();
          if(methodSymbol.name().equals("test")) {
            assertThat(methodSymbol.signature())
              .isEqualTo("org.sonar.java.resolve.targets.MethodCompleteSignature#test(SJZI[BLjava/lang/Object;CFDLjava/lang/String;)V");
          } else {
            assertThat(methodSymbol.signature())
              .isEqualTo("org.sonar.java.resolve.targets.MethodCompleteSignature#test2([Lorg/sonar/java/resolve/targets/MethodCompleteSignature;)[Lorg/sonar/java/resolve/targets/MethodCompleteSignature;");
          }
        }

      }), Collections.singletonList(new File("target/test-classes")), null));
  }

  @Test
  public void test_is_overridable() throws Exception {
    Map<Integer, Boolean> methodOverridableByLine = ImmutableMap.of(
      25, false,
      29, false,
      32, false,
      35, false,
      38, true);

    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/OverridableMethodSymbols.java"),
      new VisitorsBridge(Collections.singleton(new SubscriptionVisitor() {
        @Override
        public List<Tree.Kind> nodesToVisit() {
          return Lists.newArrayList(Tree.Kind.METHOD);
        }

        @Override
        public void visitNode(Tree tree) {
          int line = ((JavaTree) tree).getLine();
          JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) ((MethodTree) tree).symbol();
          assertThat(symbol.isOverridable()).isEqualTo(methodOverridableByLine.get(line));
        }

      }), Collections.singletonList(new File("target/test-classes")), null));
  }

}
