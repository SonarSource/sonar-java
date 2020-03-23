/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.plugins.java.api.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

public class MethodMatchersTest {

  @Test
  public void test_types() {
    String source = "" +
      /* 01 */ "interface A {\n" +
      /* 02 */ "  void f(int x);\n" +
      /* 03 */ "}\n" +
      /* 04 */ "interface B extends A {\n" +
      /* 05 */ "  void f(int x);\n" +
      /* 06 */ "}\n" +
      /* 07 */ "class X {\n" +
      /* 08 */ "  void f(int x);\n" +
      /* 09 */ "}\n" +
      /* 10 */ "class Main {\n" +
      /* 11 */ "  void main(A a, B b, X x) {\n" +
      /* 12 */ "    a.f(12);\n" +
      /* 13 */ "    b.f(12);\n" +
      /* 14 */ "    x.f(12);\n" +
      /* 15 */ "  }\n" +
      /* 16 */ "} \n";

    // exact types
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").withAnyParameters().build()))
      .containsExactly(2, 12);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("B").names("f").withAnyParameters().build()))
      .containsExactly(5, 13);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("X").names("f").withAnyParameters().build()))
      .containsExactly(8, 14);

    // sub types
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("A").names("f").withAnyParameters().build()))
      .containsExactly(2, 5, 12, 13);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("B").names("f").withAnyParameters().build()))
      .containsExactly(5, 13);

    // any types
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofAnyType().names("f").withAnyParameters().build()))
      .containsExactly(2, 5, 8, 12, 13, 14);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes(ANY).names("f").withAnyParameters().build()))
      .containsExactly(2, 5, 8, 12, 13, 14);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes(ANY).names("f").withAnyParameters().build()))
      .containsExactly(2, 5, 8, 12, 13, 14);

    // several types
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("B", "X").names("f").withAnyParameters().build()))
      .containsExactly(5, 8, 13, 14);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("A", "X").names("f").withAnyParameters().build()))
      .containsExactly(2, 5, 8, 12, 13, 14);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("B", "X").names("f").withAnyParameters().build()))
      .containsExactly(5, 8, 13, 14);
  }

  @Test
  public void test_names() {
    String source = "" +
      /* 01 */ "interface A {\n" +
      /* 02 */ "  void a(int x);\n" +
      /* 03 */ "  void aa(int x);\n" +
      /* 04 */ "  void b(int x);\n" +
      /* 05 */ "}\n" +
      /* 06 */ "class Main {\n" +
      /* 07 */ "  void main(A a) {\n" +
      /* 08 */ "    a.a(12);\n" +
      /* 09 */ "    a.aa(12);\n" +
      /* 10 */ "    a.b(12);\n" +
      /* 11 */ "    new Main();\n" +
      /* 12 */ "  }\n" +
      /* 13 */ "} \n";

    // one name
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("a").withAnyParameters().build()))
      .containsExactly(2, 8);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("aa").withAnyParameters().build()))
      .containsExactly(3, 9);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("b").withAnyParameters().build()))
      .containsExactly(4, 10);

    // several names
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("a", "b").withAnyParameters().build()))
      .containsExactly(2, 4, 8, 10);

    // start with
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").name(name -> name.startsWith("a")).withAnyParameters().build()))
      .containsExactly(2, 3, 8, 9);

    // any names
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").anyName().withAnyParameters().build()))
      .containsExactly(2, 3, 4, 8, 9, 10);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names(ANY).withAnyParameters().build()))
      .containsExactly(2, 3, 4, 8, 9, 10);

    // predicate
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").name("B"::equalsIgnoreCase).withAnyParameters().build()))
      .containsExactly(4, 10);

    // constructor
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("Main").constructor().withAnyParameters().build()))
      .containsExactly(11);
  }

  @Test
  public void test_parameters() {
    String source = "" +
      /* 01 */ "interface A { \n" +
      /* 02 */ "  void f();\n" +
      /* 03 */ "  void f(int x);\n" +
      /* 04 */ "  void f(int x, long y);\n" +
      /* 05 */ "  void f(String x);\n" +
      /* 06 */ "  static void main(A a) {\n" +
      /* 07 */ "    a.f();\n" +
      /* 08 */ "    a.f(12);\n" +
      /* 09 */ "    a.f(12, 15L);\n" +
      /* 10 */ "    java.util.function.Consumer<Integer> c = a::f;\n" +
      /* 11 */ "  }\n" +
      /* 12 */ "} \n";

    // without parameters
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addWithoutParametersMatcher().build()))
      .containsExactly(2, 7);

    // with parameters
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addParametersMatcher("int").build()))
      .containsExactly(3, 8, 10);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addParametersMatcher("int", "long").build()))
      .containsExactly(4, 9);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addParametersMatcher("int", ANY).build()))
      .containsExactly(4, 9);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addParametersMatcher(ANY).build()))
      .containsExactly(3, 5, 8, 10);

    // several with parameters
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f")
      .addParametersMatcher("int")
      .addParametersMatcher("int", "long").build()))
      .containsExactly(3, 4, 8, 9, 10);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f")
      .addWithoutParametersMatcher()
      .addParametersMatcher("int")
      .addParametersMatcher("int", "long").build()))
      .containsExactly(2, 3, 4, 7, 8, 9, 10);

    // start with parameters
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f")
      .addParametersMatcher(types -> types.size() >= 1 && types.get(0).is("int")).build()))
      .containsExactly(3, 4, 8, 9, 10);

    // with any parameters
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").withAnyParameters().build()))
      .containsExactly(2, 3, 4, 5, 7, 8, 9, 10);
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addParametersMatcher(type -> true).build()))
      .containsExactly(2, 3, 4, 5, 7, 8, 9, 10);

    // predicate
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f")
      .addParametersMatcher(types -> types.size() == 2 && types.get(0).is("int") && !types.get(1).is("int")).build()))
      .containsExactly(4, 9);
  }

  @Test
  public void test_tree_and_symbol_and_or() {
    String source = "" +
      /* 01 */ "package pkg;\n" +
      /* 02 */ "import java.util.function.*;\n" +
      /* 03 */ "class A { \n" +
      /* 04 */ "  A(int x) { }\n" +
      /* 05 */ "  void f(int x) { }\n" +
      /* 06 */ "  void main() {\n" +
      /* 07 */ "    A a = new A(12);\n" +
      /* 08 */ "    a.f(12);\n" +
      /* 09 */ "    Consumer<Integer> c = a::f;\n" +
      /* 10 */ "    Supplier<A> s = A::new;\n" +
      /* 11 */ "  }\n" +
      /* 12 */ "} \n";

    // method f(int)
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.A").names("f").addParametersMatcher("int").build()))
      .containsExactly(5, 8, 9);
    assertThat(findMatchesOnSymbol(source, MethodMatchers.create().ofTypes("pkg.A").names("f").addParametersMatcher("int").build()))
      .containsExactly(5, 8); // missing 9 because symbol.isMethodSymbol() of method reference return false

    // constructor
    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.A").constructor().addParametersMatcher("int").build()))
      .containsExactly(4, 7); // missing 10 because "A::new" is an unknown type
    assertThat(findMatchesOnSymbol(source, MethodMatchers.create().ofTypes("pkg.A").constructor().addParametersMatcher("int").build()))
      .containsExactly(4, 7);

    // or
    assertThat(findMatchesOnTree(source, MethodMatchers.or(
      MethodMatchers.create().ofTypes("pkg.A").constructor().addParametersMatcher("int").build(),
      MethodMatchers.create().ofTypes("pkg.A").names("f").addParametersMatcher("int").build())))
      .containsExactly(4, 5, 7, 8, 9);
    assertThat(findMatchesOnSymbol(source, MethodMatchers.or(
      MethodMatchers.create().ofTypes("pkg.A").constructor().addParametersMatcher("int").build(),
      MethodMatchers.create().ofTypes("pkg.A").names("f").addParametersMatcher("int").build())))
      .containsExactly(4, 5, 7, 8);

    // empty
    assertThat(findMatchesOnTree(source, MethodMatchers.none())).isEmpty();
    assertThat(findMatchesOnSymbol(source, MethodMatchers.none())).isEmpty();
  }

  @Test
  public void test_inheritance() {
    String source = "" +
      /* 01 */ "package pkg;\n" +
      /* 02 */ "class A { }\n" +
      /* 03 */ "interface I {\n" +
      /* 04 */ "  void f();\n" +
      /* 05 */ "  void f(int x);\n" +
      /* 06 */ "}\n" +
      /* 07 */ "@FunctionalInterface\n" +
      /* 08 */ "interface J {\n" +
      /* 09 */ "  void f(int x);\n" +
      /* 10 */ "}\n" +
      /* 11 */ "abstract class B extends A implements I, J {\n" +
      /* 12 */ "  public void f() { }\n" +
      /* 13 */ "}\n" +
      /* 14 */ "class C extends B {\n" +
      /* 15 */ "  @Override\n" +
      /* 16 */ "  public void f(int x) { }\n" +
      /* 17 */ "}\n" +
      /* 18 */ "class D extends C {\n" +
      /* 19 */ "  @Override\n" +
      /* 20 */ "  public void f(int x) { }\n" +
      /* 21 */ "}\n" +
      /* 22 */ "class E extends D { }\n" +
      /* 23 */ "class F extends E { }\n" +
      /* 24 */ "class Main {\n" +
      /* 25 */ "  void main(Object o, A a, B b, C c, D d, E e, F f) {\n" +
      /* 26 */ "    o.toString();\n" +
      /* 27 */ "    a.toString();\n" +
      /* 28 */ "    b.f();\n" +
      /* 29 */ "    b.f(42);\n" +
      /* 30 */ "    c.f(42);\n" +
      /* 31 */ "    d.f(42);\n" +
      /* 32 */ "    e.f(42);\n" +
      /* 33 */ "    f.f(42);\n" +
      /* 34 */ "    f.f();\n" +
      /* 35 */ "    J j = d::f;\n" +
      /* 36 */ "    j = e::f;\n" +
      /* 37 */ "    j.f(42);\n" +
      /* 38 */ "  }\n" +
      /* 39 */ "}\n";

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("java.lang.Object").names("toString").addWithoutParametersMatcher().build()))
      .containsExactly(26);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("java.lang.Object").names("toString").addWithoutParametersMatcher().build()))
      .containsExactly(26, 27);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofType(type -> type.is("pkg.B")).names("f").addWithoutParametersMatcher().build()))
      .containsExactly(12, 28);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.B").names("f").addWithoutParametersMatcher().build()))
      .containsExactly(12, 28, 34);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.B").names("f").addParametersMatcher("int").build()))
      .containsExactly(16, 20, 29, 30, 31, 32, 33, 35, 36);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.F").names("f").addWithoutParametersMatcher().build()))
      .containsExactly(34);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.A").names("f").addParametersMatcher("int").build()))
      .isEmpty();

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.B").names("f").addParametersMatcher("int").build()))
      .containsExactly(29);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.C").names("f").addParametersMatcher("int").build()))
      .containsExactly(16, 30);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.D").names("f").addParametersMatcher("int").build()))
      .containsExactly(20, 31, 32, 33, 35, 36);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.D").names("f").addParametersMatcher("int").build()))
      .containsExactly(20, 31, 35);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.E").names("f").addParametersMatcher("int").build()))
      .containsExactly(32, 36);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("pkg.F").names("f").addParametersMatcher("int").build()))
      .containsExactly(33);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.A").names("f").addParametersMatcher("int").build()))
      .containsExactly(16, 20, 29, 30, 31, 32, 33, 35, 36);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.I").names("f").addParametersMatcher("int").build()))
      .containsExactly(5, 16, 20, 29, 30, 31, 32, 33, 35, 36);

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofSubTypes("pkg.J").names("f").addParametersMatcher("int").build()))
      .containsExactly(9, 16, 20, 29, 30, 31, 32, 33, 35, 36, 37);
  }

  @Test(expected = IllegalStateException.class)
  public void no_types() {
    MethodMatchers.create().ofTypes().anyName().withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void null_type_predicate() {
    MethodMatchers.create().ofType(null).anyName().withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void no_subtypes() {
    MethodMatchers.create().ofSubTypes().anyName().withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void invalid_any_type() {
    MethodMatchers.create().ofTypes("A", ANY).anyName().withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void no_name() {
    MethodMatchers.create().ofAnyType().names().withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void null_name_predicate() {
    MethodMatchers.create().ofAnyType().name(null).withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void invalid_any_name() {
    MethodMatchers.create().ofAnyType().names("A", ANY).withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void invalid_any_parameters() {
    MethodMatchers.create().ofAnyType().anyName().addParametersMatcher("int").withAnyParameters().build();
  }

  @Test(expected = IllegalStateException.class)
  public void null_parameter_predicate() {
    MethodMatchers.create().ofAnyType().anyName().addParametersMatcher((Predicate<List<Type>>) null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void invalid_matcher_without_parameters() {
    MethodMatchers.create().ofAnyType().anyName().build();
  }

  @Test
  public void test_method_selector_and_method_identifier() {
    String source = "" +
      /* 01 */ "class A {\n" +
      /* 02 */ "  void f(A a) {\n" +
      /* 03 */ "    f(this);\n" +
      /* 04 */ "    a.f(a);\n" +
      /* 05 */ "  }\n" +
      /* 06 */ "}\n";

    assertThat(findMatchesOnTree(source, MethodMatchers.create().ofTypes("A").names("f").addParametersMatcher("A").build()))
      .containsExactly(2, 3, 4);
  }

  private static List<Integer> findMatchesOnTree(String fileContent, MethodMatchers matcher) {
    return findMatches(fileContent, matcher, false);
  }

  private static List<Integer> findMatchesOnSymbol(String fileContent, MethodMatchers matcher) {
    return findMatches(fileContent, matcher, true);
  }

  private static List<Integer> findMatches(String fileContent, MethodMatchers matcher, boolean useSymbol) {
    Visitor visitor = new Visitor(matcher, useSymbol);
    JavaAstScanner.scanSingleFileForTests(
      inputFile(fileContent),
      new VisitorsBridge(Collections.singletonList(visitor), new ArrayList<>(), null));
    return visitor.matches;
  }

  private static InputFile inputFile(String fileContent) {
    return new TestInputFileBuilder("", "TestFile.java")
      .setContents(fileContent)
      .setCharset(UTF_8)
      .setLanguage("java")
      .build();
  }

  private static class Visitor extends SubscriptionVisitor {

    public MethodMatchers matcher;
    private boolean useSymbol;
    public List<Integer> matches = new ArrayList<>();

    public Visitor(MethodMatchers matcher, boolean useSymbol) {
      this.matcher = matcher;
      this.useSymbol = useSymbol;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE);
    }

    @Override
    public void visitNode(Tree tree) {
      super.visitNode(tree);
      boolean match = false;
      if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        if (useSymbol) {
          match = matcher.matches(((MethodInvocationTree) tree).symbol());
        } else {
          match = matcher.matches((MethodInvocationTree) tree);
        }
      } else if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        if (useSymbol) {
          match = matcher.matches(((MethodTree) tree).symbol());
        } else {
          match = matcher.matches((MethodTree) tree);
        }
      } else if (tree.is(Tree.Kind.NEW_CLASS)) {
        if (useSymbol) {
          match = matcher.matches(((NewClassTree) tree).constructorSymbol());
        } else {
          match = matcher.matches((NewClassTree) tree);
        }
      } else if (tree.is(Tree.Kind.METHOD_REFERENCE)) {
        if (useSymbol) {
          match = matcher.matches(((MethodReferenceTree) tree).symbolType().symbol());
        } else {
          match = matcher.matches((MethodReferenceTree) tree);
        }
      }
      if (match) {
        matches.add(((JavaTree) tree).getLine());
      }
    }
  }

}
