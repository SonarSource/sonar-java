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
package org.sonar.java.se;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.java.se.utils.SETestUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNullable;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedWithStrongNullness;
import static org.sonar.java.se.NullableAnnotationUtils.nonNullAnnotation;
import static org.sonar.java.se.NullableAnnotationUtils.nullableAnnotation;

class NullableAnnotationUtilsTest {

  private static Collection<Symbol> memberSymbols;

  @BeforeAll
  static void beforeAll() {
    memberSymbols = SETestUtils.getSemanticModel("src/test/files/se/annotations/NullableAnnotationUtils.java")
      .getClassType("android.support.annotation.A")
      .symbol()
      .memberSymbols();
  }

  @Test
  void private_constructor() throws Exception {
    assertThat(Modifier.isFinal(NullableAnnotationUtils.class.getModifiers())).isTrue();
    Constructor<NullableAnnotationUtils> constructor = NullableAnnotationUtils.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void global_annotation() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse(new File("src/test/files/se/annotations/NullableAnnotationUtils_global.java"));

    List<Symbol.MethodSymbol> nonNullParameterMethods = Collector.methodsWithName("nonNull", cut);
    assertThat(nonNullParameterMethods)
      .hasSize(6)
      .allMatch(NullableAnnotationUtils::isGloballyAnnotatedParameterNonNull)
      .noneMatch(NullableAnnotationUtils::isGloballyAnnotatedParameterNullable);

    List<Symbol.MethodSymbol> nullableParameterMethods = Collector.methodsWithName("nullable", cut);
    assertThat(nullableParameterMethods)
      .hasSize(1)
      .allMatch(NullableAnnotationUtils::isGloballyAnnotatedParameterNullable)
      .noneMatch(NullableAnnotationUtils::isGloballyAnnotatedParameterNonNull);

    List<Symbol> nonNullFields = Collector.variablesWithName("nonNull", cut);
    assertThat(nonNullFields)
      .hasSize(4)
      .allMatch(NullableAnnotationUtils::isAnnotatedNonNull);
    assertThat(nonNullFields.stream().map(Symbol::metadata))
      .noneMatch(NullableAnnotationUtils::isAnnotatedNullable);

    List<Symbol.MethodSymbol> nonNullReturnTypeMethods = Collector.methodsWithName("returnType", cut);
    assertThat(nonNullReturnTypeMethods)
      .hasSize(5)
      .allMatch(NullableAnnotationUtils::isAnnotatedNonNull);
    assertThat(nonNullReturnTypeMethods.stream().map(Symbol::metadata))
      .noneMatch(NullableAnnotationUtils::isAnnotatedNullable);
  }

  private static class Collector extends BaseTreeVisitor {

    private final String target;
    private final List<Symbol.MethodSymbol> methods = new ArrayList<>();
    private final List<Symbol> variables = new ArrayList<>();

    private Collector(String target) {
      this.target = target;
    }

    @Override
    public void visitAnnotation(AnnotationTree annotationTree) {
      // skip annotations
    }

    @Override
    public void visitMethod(MethodTree tree) {
      if (tree.simpleName().name().toLowerCase().contains(target.toLowerCase())) {
        methods.add(tree.symbol());
      }
      super.visitMethod(tree);
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (tree.simpleName().name().toLowerCase().contains(target.toLowerCase())) {
        variables.add(tree.symbol());
      }
      super.visitVariable(tree);
    }

    static List<Symbol.MethodSymbol> methodsWithName(String target, Tree tree) {
      Collector visitor = new Collector(target);
      tree.accept(visitor);
      return visitor.methods;
    }

    static List<Symbol> variablesWithName(String target, Tree tree) {
      Collector visitor = new Collector(target);
      tree.accept(visitor);
      return visitor.variables;
    }
  }

  @Test
  void testIsAnnotatedNullable() {
    Symbol foo = getSymbol("foo");
    assertThat(isAnnotatedNullable(foo.metadata())).isFalse();

    getSymbols("nullable").forEach(s -> {
      assertThat(isAnnotatedNullable(s.metadata())).as(s.name() + " should be recognized as Nullable.").isTrue();
      assertThat(isAnnotatedNonNull(s)).as(s.name() + " should NOT be recognized as Nonnull.").isFalse();
    });
  }

  @Test
  void testIsAnnotatedNonNull() {
    Symbol foo = getSymbol("foo");
    assertThat(isAnnotatedNonNull(foo)).isFalse();

    getSymbols("nonnull").forEach(s -> {
      assertThat(isAnnotatedNonNull(s)).as(s.name() + " should be recognized as Nonnull.").isTrue();
      assertThat(isAnnotatedNullable(s.metadata())).as(s.name() + " should NOT be recognized as Nullable.").isFalse();
    });

    assertThat(isAnnotatedNonNull(getSymbol("nullable6"))).isFalse();
    assertThat(isAnnotatedNonNull(getSymbol("nullable7"))).isFalse();
  }

  @Test
  void testNullableAnnotationOnModifiers() {
    assertThat(nullableAnnotation(getMethodTree("foo").modifiers())).isNotPresent();
    assertThat(nullableAnnotation(getMethodTree("bar").modifiers())).isNotPresent();
    assertThat(nullableAnnotation(getMethodTree("nullable1").modifiers()))
      .hasValueSatisfying(annotation -> assertThat(annotation.annotationType().symbolType().fullyQualifiedName())
        .isEqualTo("javax.annotation.Nullable"));
    assertThat(nullableAnnotation(getMethodTree("nullable2").modifiers()))
      .hasValueSatisfying(annotation -> assertThat(annotation.annotationType().symbolType().fullyQualifiedName())
        .isEqualTo("javax.annotation.CheckForNull"));
  }

  @Test
  void testNonNullAnnotationOnModifiers() {
    assertThat(nonNullAnnotation(getMethodTree("foo").modifiers())).isNotPresent();
    assertThat(nonNullAnnotation(getMethodTree("bar").modifiers())).isNotPresent();
    assertThat(nonNullAnnotation(getMethodTree("nullable1").modifiers())).isNotPresent();
    assertThat(nonNullAnnotation(getMethodTree("nullable6").modifiers())).isNotPresent();
    assertThat(nonNullAnnotation(getMethodTree("nonnull1").modifiers()))
      .hasValueSatisfying(annotation -> assertThat(annotation.annotationType().symbolType().fullyQualifiedName())
        .isEqualTo("javax.annotation.Nonnull"));
  }

  @Test
  void testNonNullAnnotationOnMetadata() {
    assertThat(nonNullAnnotation(getSymbol("nullable1").metadata())).isNull();
    assertThat(nonNullAnnotation(getSymbol("nullable6").metadata())).isNull();
    assertThat(nonNullAnnotation(getSymbol("nonnull1").metadata())).isEqualTo("javax.annotation.Nonnull");
  }

  @Test
  void testNonNullAnnotationOnSymbol() {
    assertThat(nonNullAnnotation(getSymbol("nullable1"))).isNull();
    assertThat(nonNullAnnotation(getSymbol("nullable6"))).isNull();
    assertThat(nonNullAnnotation(getSymbol("nonnull1"))).isEqualTo("javax.annotation.Nonnull");
  }

  @Test
  void testStrongNullness() {
    assertThat(isAnnotatedWithStrongNullness(getSymbol("nullable1").metadata())).isFalse();
    assertThat(isAnnotatedWithStrongNullness(getSymbol("nullable2").metadata())).isTrue();
  }

  private static MethodTree getMethodTree(String name) {
    return ((MethodTree) getSymbol(name).declaration());
  }

  private static Symbol getSymbol(String name) {
    return memberSymbols.stream().filter(s -> name.equals(s.name())).findAny().get();
  }

  private static Stream<Symbol> getSymbols(String nameStartsWith) {
    return memberSymbols.stream().filter(s -> s.name().startsWith(nameStartsWith));
  }
}
