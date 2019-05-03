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
package org.sonar.java.resolve.targets.bytecodeGenerics;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.java.resolve.TypeVariableJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericsSubstitutionFromBytecode {

  @Test
  public void verify_generic_substitution() {
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/bytecodeGenerics/MyImpl.java"),
      new VisitorsBridge(
        Collections.singletonList(new MyVisitor()),
        Collections.singletonList(new File("target/test-classes")),
        null));
  }

  private static class MyVisitor extends IssuableSubscriptionVisitor {

    JavaType substitution;
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Arrays.asList(Tree.Kind.ASSIGNMENT, Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      if(tree.is(Tree.Kind.CLASS)) {
        ClassTree classTree = (ClassTree) tree;
        JavaSymbol QSymbol = ((JavaType) classTree.superClass().symbolType()).getSymbol().typeParameters().lookup("Q").iterator().next();
        substitution = ((ParametrizedTypeJavaType) classTree.superClass().symbolType()).substitution((TypeVariableJavaType) QSymbol.type());
        assertThat(substitution).isNotNull();
      } else {
        AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) tree;
        assertThat(substitution).isNotNull();
        assertThat(assignmentExpressionTree.variable().symbolType()).isSameAs(substitution);
      }
    }
  }
}
