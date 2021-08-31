/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApiCheckerTest {

  private CompilationUnitTree cut;

  @BeforeEach
  public void setUp() {
    cut = JParserTestUtils.parse(new File("src/test/files/ast/PublicApi.java"));
  }

  @Test
  void private_constructor() throws Exception {
    Constructor<PublicApiChecker> constructor = PublicApiChecker.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void targeted_kinds() {
    assertThat(PublicApiChecker.classKinds())
      .hasSize(5)
      .containsExactlyInAnyOrder(Tree.Kind.ANNOTATION_TYPE, Tree.Kind.ENUM, Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.RECORD);

    assertThat(PublicApiChecker.methodKinds())
      .hasSize(2)
      .containsExactlyInAnyOrder(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD);

    assertThat(PublicApiChecker.apiKinds())
      .hasSize(8)
      .contains(PublicApiChecker.classKinds())
      .contains(PublicApiChecker.methodKinds())
      .contains(Tree.Kind.VARIABLE);
  }

  @Nested
  class SpecialCases {
    @Test
    void getApiJavadoc_parametrized_method() {
      CompilationUnitTree cut = JParserTestUtils.parse("interface A {\n"
        + "  /**\n   * documented\n   */\n"
        + "  <T> void foo(T t);\n"
        + "}");
      Optional<String> apiJavadoc = PublicApiChecker.getApiJavadoc(((ClassTree)(cut.types().get(0))).members().get(0));
      assertThat(apiJavadoc)
        .isPresent()
        .contains("/**\n   * documented\n   */");
    }

    @Test
    void getApiJavadoc_constructor() {
      CompilationUnitTree cut = JParserTestUtils.parse("public class A { public A() {} }");
      Optional<String> apiJavadoc = PublicApiChecker.getApiJavadoc(((ClassTree) cut.types().get(0)).members().get(0));
      assertThat(apiJavadoc).isNotPresent();
    }

    @Test
    void empty_default_constructors_is_not_public_api() {
      ClassTree a = (ClassTree) JParserTestUtils.parse("public class A { public A() { } }").types().get(0);
      assertThat(PublicApiChecker.isPublicApi(a, a.members().get(0))).isFalse();

      ClassTree b = (ClassTree) JParserTestUtils.parse("public class B { public B() { foo(); } }").types().get(0);
      assertThat(PublicApiChecker.isPublicApi(b, b.members().get(0))).isTrue();
    }

    @Test
    void public_class_is_public_api() {
      CompilationUnitTree cut = JParserTestUtils.parse("package org.foo; public class A { }");
      Tree a = cut.types().get(0);
      assertThat(PublicApiChecker.isPublicApi(cut, a)).isTrue();
      assertThat(PublicApiChecker.isPublicApi(null, a)).isTrue();
      assertThat(PublicApiChecker.isPublicApi(a, a)).isTrue();
    }

    @Test
    void inner_class_of_public_method_is_not_public_api() {
      CompilationUnitTree cut = JParserTestUtils.parse("public class A { public void m() { class B {} } }");
      ClassTree a = (ClassTree) cut.types().get(0);
      MethodTree m = (MethodTree) a.members().get(0);
      ClassTree b = (ClassTree) m.block().body().get(0);

      assertThat(PublicApiChecker.isPublicApi(null, a)).isTrue();
      assertThat(PublicApiChecker.isPublicApi(a, m)).isTrue();
      assertThat(PublicApiChecker.isPublicApi(m, b)).isFalse();
    }

    @Test
    void inner_class_of_interface_is_public_api() {
      CompilationUnitTree cut = JParserTestUtils.parse("public interface A { class B {} }");
      ClassTree a = (ClassTree) cut.types().get(0);
      ClassTree b = (ClassTree) a.members().get(0);

      assertThat(PublicApiChecker.isPublicApi(null, a)).isTrue();
      assertThat(PublicApiChecker.isPublicApi(a, b)).isTrue();
    }

    @Test
    void two_javadoc_comment() {
      CompilationUnitTree cut = JParserTestUtils.parse("/**\n" +
        "* dandling javadoc\n" +
        "*/\n" +
        "/**\n" +
        "* documented\n" +
        "*/\n" +
        "class A { }");
      Optional<String> apiJavadoc = PublicApiChecker.getApiJavadoc(cut.types().get(0));
      assertThat(apiJavadoc)
        .isPresent()
        .contains("/**\n* documented\n*/");
    }
  }

  @Test
  void isPublicApiAccessorsHandledAsMethods() {
    SubscriptionVisitor visitor = getPublicApiVisitor();
    visitor.scanTree(cut);
  }

  private SubscriptionVisitor getPublicApiVisitor() {
    return new SubscriptionVisitor() {

      private final Deque<ClassTree> classTrees = new LinkedList<>();
      private final Deque<MethodTree> methodTrees = new LinkedList<>();

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
  void retrieveJavadoc() {
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
      assertThat(PublicApiChecker.getApiJavadoc(tree)).as(name).isPresent();
    } else {
      assertThat(PublicApiChecker.getApiJavadoc(tree)).isNotPresent();
    }
  }

}
