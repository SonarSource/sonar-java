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
package org.sonar.java.ast.visitors;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PublicApiCheckerTest {

  private CompilationUnitTree cut;

  @Before
  public void setUp() {
    ActionParser p = JavaParser.createParser();
    cut = (CompilationUnitTree) p.parse(new File("src/test/files/ast/PublicApi.java"));
  }

  @Test
  public void isPublicApiAccessorsHandledAsMethods() {
    SubscriptionVisitor visitor = getPublicApiVisitor();
    visitor.scanTree(cut);
  }

  private SubscriptionVisitor getPublicApiVisitor() {
    return new SubscriptionVisitor() {

      private final Deque<ClassTree> classTrees = Lists.newLinkedList();
      private final Deque<MethodTree> methodTrees = Lists.newLinkedList();

      @Override
      public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.values());
      }

      @Override
      public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.VARIABLE)) {
          VariableTree variableTree = (VariableTree) tree;
          String name = variableTree.simpleName().name();
          Tree parent = classTrees.peek();
          if (!methodTrees.isEmpty()) {
            parent = methodTrees.peek();
          }
          assertThat(PublicApiChecker.isPublicApi(parent, tree)).as(name).isEqualTo(name.endsWith("Public"));
        } else if (tree.is(PublicApiChecker.methodKinds())) {
          MethodTree methodTree = (MethodTree) tree;
          methodTrees.push(methodTree);
          String name = methodTree.simpleName().name();
          // getters and setters are included in the public API
          assertThat(PublicApiChecker.isPublicApi(classTrees.peek(), tree)).as(name).isEqualTo(name.endsWith("Public") || name.contains("GetSet"));
        } else if (tree.is(PublicApiChecker.classKinds())) {
          IdentifierTree className = ((ClassTree) tree).simpleName();
          if(className==null) {
            assertThat(PublicApiChecker.isPublicApi(classTrees.peek(), tree)).isFalse();
          }else {
            assertThat(PublicApiChecker.isPublicApi(classTrees.peek(), tree)).as(className.name()).isEqualTo(className != null && className.name().endsWith("Public"));
          }
          classTrees.push((ClassTree) tree);
        } else {
          assertThat(PublicApiChecker.isPublicApi(classTrees.peek(), tree)).isFalse();
        }
      }

      @Override
      public void leaveNode(Tree tree) {
        if (tree.is(PublicApiChecker.classKinds())) {
          classTrees.pop();
        } else if (tree.is(PublicApiChecker.methodKinds())) {
          methodTrees.pop();
        }
      }
    };
  }

  @Test
  public void retrieveJavadoc() {
    new SubscriptionVisitor() {

      @Override
      public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.values());
      }

      @Override
      public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.VARIABLE)) {
          VariableTree variableTree = (VariableTree) tree;
          checkApi(tree, variableTree.simpleName().name());
        } else if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
          MethodTree methodTree = (MethodTree) tree;
          checkApi(tree, methodTree.simpleName().name());
        } else if (tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
          IdentifierTree idTree = ((ClassTree) tree).simpleName();
          checkApi(tree, idTree == null ? "" : idTree.name());
        } else {
          checkApi(tree, "");
        }
      }
    }.scanTree(cut);

  }

  private void checkApi(Tree tree, String name) {
    if (name.startsWith("documented")) {
      assertThat(PublicApiChecker.getApiJavadoc(tree)).as(name).isNotNull();
    } else {
      assertThat(PublicApiChecker.getApiJavadoc(tree)).isNull();
    }
  }

}
