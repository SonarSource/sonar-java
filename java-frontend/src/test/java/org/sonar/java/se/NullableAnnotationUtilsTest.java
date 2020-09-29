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
package org.sonar.java.se;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNullable;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedWithStrongNullness;
import static org.sonar.java.se.NullableAnnotationUtils.isGloballyAnnotatedParameterNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.nonNullAnnotation;
import static org.sonar.java.se.NullableAnnotationUtils.nullableAnnotation;

class NullableAnnotationUtilsTest {

  private static Sema semanticModel;

  @BeforeAll
  static void beforeAll() {
    semanticModel = SETestUtils.getSemanticModel("src/test/files/se/annotations/NullableAnnotationUtils.java");
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
  @Disabled("flickering NPE")
  void testEclipseIsGloballyAnnotatedNonNull() {
    List<File> classPath = new ArrayList<>(FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar", "zip"}, true));
    classPath.add(new File("target/test-classes"));
    // adding the class corresponding to package-info having @NonNullByDefault annotation
    classPath.add(new File("src/test/files/se/annotations/eclipse"));

    Sema semanticModel = getSemanticModel("src/test/files/se/annotations/eclipse/org/foo/bar/Eclipse.java", classPath);
    getMethods(semanticModel, "org.foo.bar.A").forEach(NullableAnnotationUtilsTest::testMethods);
    getMethods(semanticModel, "org.foo.bar.B").forEach(NullableAnnotationUtilsTest::testMethods);

    // fields not handled
    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.bar.B", "field"))).isFalse();

    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.bar.C", "getStringNullable"))).isFalse();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.bar.C", "getStringNullable"))).isNull();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.bar.C", "getStringNullable").metadata())).isNull();

    semanticModel = getSemanticModel("src/test/files/se/annotations/eclipse/org/foo/foo/Eclipse.java", classPath);
    getMethods(semanticModel, "org.foo.foo.A").forEach(NullableAnnotationUtilsTest::testMethods);

    semanticModel = getSemanticModel("src/test/files/se/annotations/eclipse/org/foo/qix/Eclipse.java", classPath);
    getMethods(semanticModel, "org.foo.qix.A").forEach(NullableAnnotationUtilsTest::testMethods);
  }

  @Test
  @Disabled("flickering NPE")
  void testSpringIsPackageAnnotatedNonNull() {
    List<File> classPath = new ArrayList<>(FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar", "zip"}, true));
    classPath.add(new File("target/test-classes"));
    // adding the class corresponding to package-info having @NonNullApi and @NonNullFields annotations
    classPath.add(new File("src/test/files/se/annotations/springframework"));

    Sema semanticModel = getSemanticModel("src/test/files/se/annotations/springframework/org/foo/bar/Spring.java", classPath);
    getMethods(semanticModel, "org.foo.bar.A").forEach(NullableAnnotationUtilsTest::testMethods);
    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.bar.B", "field"))).isFalse();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.bar.B", "field"))).isNull();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.bar.B", "field").metadata())).isNull();
    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.bar.C", "getStringNullable"))).isFalse();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.bar.C", "getStringNullable"))).isNull();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.bar.C", "getStringNullable").metadata())).isNull();

    semanticModel = getSemanticModel("src/test/files/se/annotations/springframework/org/foo/foo/Spring.java", classPath);
    getMethods(semanticModel, "org.foo.foo.A").forEach(NullableAnnotationUtilsTest::testMethods);
    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.foo.B", "field1"))).isTrue();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.foo.B", "field1"))).isEqualTo("org.springframework.lang.NonNullFields");
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.foo.B", "field1").metadata())).isNull(); // @NonNullFields declared at package level
    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.foo.B", "field2"))).isFalse();
    assertThat(nonNullAnnotation(getSymbol(semanticModel, "org.foo.foo.B", "field2"))).isNull();
  }

  private static Sema getSemanticModel(String fileName, List<File> classPath) {
    File file = new File(fileName);
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(file, classPath);
    return cut.sema;
  }

  private static void testMethods(Symbol.MethodSymbol s) {
    String name = s.name();
    boolean annotatedNonNull = isAnnotatedNonNull(s);
    if (StringUtils.containsIgnoreCase(name, "ReturnNonNull")) {
      assertThat(annotatedNonNull).as(s.name() + " should be recognized as returning NonNull.").isTrue();
    } else {
      assertThat(annotatedNonNull).as(s.name() + " should NOT be recognized as returning NonNull.").isFalse();
    }
    boolean globallyAnnotatedParameterNonNull = isGloballyAnnotatedParameterNonNull(s);
    if (StringUtils.containsIgnoreCase(name, "nonNullParameters")) {
      assertThat(globallyAnnotatedParameterNonNull).as(s.name() + " should be recognized as NonNull for parameters.").isTrue();
    } else {
      assertThat(globallyAnnotatedParameterNonNull).as(s.name() + " should NOT be recognized as NonNull for parameters.").isFalse();
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
    return getMainType().symbol().memberSymbols().stream().filter(s -> name.equals(s.name())).findAny().get();
  }

  private static Stream<Symbol> getSymbols(String nameStartsWith) {
    return getMainType().symbol().memberSymbols().stream().filter(s -> s.name().startsWith(nameStartsWith));
  }

  private static Type getMainType() {
    return semanticModel.getClassType("android.support.annotation.A");
  }

  private static Symbol getSymbol(Sema semanticModel, String owner, String name) {
    return semanticModel.getClassType(owner).symbol().memberSymbols().stream().filter(s -> name.equals(s.name())).findAny().get();
  }

  private static Stream<Symbol.MethodSymbol> getMethods(Sema semanticModel, String owner) {
    return semanticModel.getClassType(owner).symbol().memberSymbols().stream()
      .filter(s -> s.isMethodSymbol())
      .map(Symbol.MethodSymbol.class::cast)
      .filter(s -> !"<init>".equals(s.name()));
  }
}
