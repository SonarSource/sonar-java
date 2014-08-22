/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.impl.Parser;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTreeMaker;
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

import static org.fest.assertions.Assertions.assertThat;

public class PublicApiCheckerTest {

  private PublicApiChecker publicApiChecker;
  private CompilationUnitTree cut;

  @Before
  public void setUp() {
    Parser p = JavaParser.createParser(Charsets.UTF_8, true);
    publicApiChecker = new PublicApiChecker();
    cut = new JavaTreeMaker().compilationUnit(p.parse(new File("src/test/files/ast/PublicApi.java")));
  }

  @Test
  public void isPublicApi() {
    new SubscriptionVisitor() {

      private Deque<ClassTree> classTrees = Lists.newLinkedList();

      @Override
      public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.values());
      }

      @Override
      public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.VARIABLE)) {
          VariableTree variableTree = (VariableTree) tree;
          String name = variableTree.simpleName().name();
          assertThat(publicApiChecker.isPublicApi(classTrees.peek(), tree)).as(name).isEqualTo(name.endsWith("Public"));
        } else if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
          MethodTree methodTree = (MethodTree) tree;
          String name = methodTree.simpleName().name();
          assertThat(publicApiChecker.isPublicApi(classTrees.peek(), tree)).as(name).isEqualTo(name.endsWith("Public"));
        } else if (tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
          classTrees.push((ClassTree) tree);
          IdentifierTree className = classTrees.peek().simpleName();
          assertThat(publicApiChecker.isPublicApi(classTrees.peek(), tree)).isEqualTo(className != null && className.name().endsWith("Public"));
        } else {
          assertThat(publicApiChecker.isPublicApi(classTrees.peek(), tree)).isFalse();
        }
      }

      @Override
      public void leaveNode(Tree tree) {
        if (tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)) {
          classTrees.pop();
        }
      }
    }.scanTree(cut);
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
          checkApi(tree, idTree==null ? "":idTree.name() );
        }else {
          checkApi(tree, "");
        }
      }
    }.scanTree(cut);

  }

  private void checkApi(Tree tree, String name) {
    if(name.startsWith("documented")) {
      assertThat(publicApiChecker.getApiJavadoc(tree)).as(name).isNotNull();
    }else {
      assertThat(publicApiChecker.getApiJavadoc(tree)).isNull();
    }
  }
}