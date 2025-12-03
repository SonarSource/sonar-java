/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.model.declaration;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.assertions.TreeAssert.assertThat;

class ClassTreeImplTest {

  @Test
  void getLine() {
    CompilationUnitTree tree = JParserTestUtils.parse("""
      class A {
        A a = new A() {};
      }
      """);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    assertThat(((JavaTree) classTree).getLine()).isEqualTo(1);
    //get line of anonymous class
    NewClassTree newClassTree = (NewClassTree) ((VariableTree) classTree.members().get(0)).initializer();
    assertThat(((JavaTree)newClassTree.classBody()).getLine()).isEqualTo(2);
  }

  @Test
  void at_token() {
    List<Tree> types = JParserTestUtils.parse("interface A {}\n @interface B {}").types();
    ClassTreeImpl interfaceType = (ClassTreeImpl) types.get(0);
    assertThat(interfaceType.atToken()).isNull();
    ClassTreeImpl annotationType = (ClassTreeImpl) types.get(1);
    assertThat(annotationType.atToken()).isNotNull();
  }

  @Test
  void records_baseVisitor() {
    ClassTree classTree = (ClassTree) JParserTestUtils.parse(
      // header
      "record A "
        // components
        + "(Object o, String s) {\n"
        // compact constructor
        + "  A { }\n"
        + "}").types().get(0);

    assertThat(classTree.declarationKeyword()).is("record");

    List<String> componentsVariableNames = new ArrayList<>();
    BaseTreeVisitor baseTreeVisitor = new BaseTreeVisitor() {
      @Override
      public void visitVariable(VariableTree tree) {
        componentsVariableNames.add(tree.simpleName().name());
      }
    };
    classTree.accept(baseTreeVisitor);

    assertThat(componentsVariableNames).containsExactly("o", "s");
  }

  @Test
  void records_members_order() {
    ClassTree classTree = (ClassTree) JParserTestUtils.parse("""
          record Output(String title, String summary, String text) {
            public Output {}
            public static final String CONST_1 = "abc";
            boolean isTooLong() { return true; }
            public static final int CONST_2 = 42;
            Output(String s) { this(s, s, s); }
            public static final boolean CONST_3 = false;
            class Inner {}
            public static final Object CONST_4 = null;
          }
          """)
      .types().get(0);

    assertThat(classTree).is(Tree.Kind.RECORD);
    List<String> membersKinds = classTree.members().stream().map(Tree::kind).map(Tree.Kind::name).toList();
    assertThat(membersKinds).containsExactly(
      "CONSTRUCTOR",
      "VARIABLE",
      "METHOD",
      "VARIABLE",
      "CONSTRUCTOR",
      "VARIABLE",
      "CLASS",
      "VARIABLE");
  }
}
