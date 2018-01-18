/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNullable;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedWith;
import static org.sonar.java.se.NullableAnnotationUtils.isGloballyAnnotatedParameterNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.isGloballyAnnotatedWith;
import static org.sonar.java.se.NullableAnnotationUtils.valuesForGlobalAnnotation;

public class NullableAnnotationUtilsTest {

  private static SemanticModel semanticModel;

  @BeforeClass
  public static void setUp() {
    semanticModel = SETestUtils.getSemanticModel("src/test/files/se/annotations/NullableAnnotationUtils.java");
  }

  @Test
  public void private_constructor() throws Exception {
    assertThat(Modifier.isFinal(NullableAnnotationUtils.class.getModifiers())).isTrue();
    Constructor<NullableAnnotationUtils> constructor = NullableAnnotationUtils.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void testIsAnnotatedWith() {
    Symbol foo = getSymbol("foo");
    assertThat(isAnnotatedWith(foo, "android.support.annotation.MyAnnotation")).isTrue();
    // not resolved
    assertThat(isAnnotatedWith(foo, "org.bar.MyOtherAnnotation")).isFalse();
  }

  @Test
  public void testIsGloballyAnnotatedWith() {
    Symbol.MethodSymbol foo = (Symbol.MethodSymbol) getSymbol("foo");
    assertThat(isGloballyAnnotatedWith(foo, "android.support.annotation.MyAnnotation")).isTrue();

    Symbol.MethodSymbol bar = (Symbol.MethodSymbol) getSymbol("bar");
    assertThat(isAnnotatedWith(bar, "android.support.annotation.MyAnnotation")).isFalse();
    assertThat(isGloballyAnnotatedWith(bar, "android.support.annotation.MyAnnotation")).isTrue();
    assertThat(isGloballyAnnotatedWith(bar, "org.bar.MyOtherAnnotation")).isFalse();
  }

  @Test
  public void testEclipseIsGloballyAnnotatedNonNull() {
    List<File> classPath = new ArrayList<>(FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar", "zip"}, true));
    classPath.add(new File("target/test-classes"));
    // adding the class corresponding to package-info having @NonNullByDefault annotation
    classPath.add(new File("src/test/files/se/annotations/eclipse"));

    SemanticModel semanticModel = getSemanticModel("src/test/files/se/annotations/eclipse/org/foo/bar/Eclipse.java", classPath);
    getMethods(semanticModel, "org.foo.bar.A").forEach(NullableAnnotationUtilsTest::testMethods);
    getMethods(semanticModel, "org.foo.bar.B").forEach(NullableAnnotationUtilsTest::testMethods);

    // fields not handled
    assertThat(isAnnotatedNonNull(getSymbol(semanticModel, "org.foo.bar.B", "field"))).isFalse();

    semanticModel = getSemanticModel("src/test/files/se/annotations/eclipse/org/foo/foo/Eclipse.java", classPath);
    getMethods(semanticModel, "org.foo.foo.A").forEach(NullableAnnotationUtilsTest::testMethods);

    semanticModel = getSemanticModel("src/test/files/se/annotations/eclipse/org/foo/qix/Eclipse.java", classPath);
    getMethods(semanticModel, "org.foo.qix.A").forEach(NullableAnnotationUtilsTest::testMethods);
  }

  private static SemanticModel getSemanticModel(String fileName, List<File> classPath) {
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(new File(fileName));
    SemanticModel semanticModel = SemanticModel.createFor(cut, new SquidClassLoader(classPath));
    return semanticModel;
  }

  private static void testMethods(Symbol.MethodSymbol s) {
    String name = s.name();
    boolean annotatedNonNull = isAnnotatedNonNull(s);
    if (StringUtils.containsIgnoreCase(name, "ReturnNonNull")) {
      assertThat(annotatedNonNull).as(s + " should be recognized as returning NonNull.").isTrue();
    } else {
      assertThat(annotatedNonNull).as(s + " should NOT be recognized as returning NonNull.").isFalse();
    }
    boolean globallyAnnotatedParameterNonNull = isGloballyAnnotatedParameterNonNull(s);
    if (StringUtils.containsIgnoreCase(name, "nonNullParameters")) {
      assertThat(globallyAnnotatedParameterNonNull).as(s + " should be recognized as NonNull for parameters.").isTrue();
    } else {
      assertThat(globallyAnnotatedParameterNonNull).as(s + " should NOT be recognized as NonNull for parameters.").isFalse();
    }
  }

  @Test
  public void testGetAnnotationValue() {
    String myClass = "org.sonar.java.resolve.targets.annotations.MyClass";
    String myAnnotation = "org.sonar.java.resolve.targets.annotations.MyAnnotation";
    String myOtherAnnotation = "org.bar.MyOtherAnnotation";
    SemanticModel semanticModel = SETestUtils.getSemanticModel("src/test/java/org/sonar/java/resolve/targets/annotations/MyClass.java");

    Symbol.MethodSymbol foo = (Symbol.MethodSymbol) getSymbol(semanticModel, myClass, "foo");
    assertThat(isAnnotatedWith(foo, myAnnotation)).isFalse();
    assertThat(isGloballyAnnotatedWith(foo, myAnnotation)).isTrue();
    // annotation value retrieved from bytecode, on package
    List<SymbolMetadata.AnnotationValue> fooAnnotationValues = valuesForGlobalAnnotation(foo, myAnnotation);
    assertThat(fooAnnotationValues).hasSize(1);
    assertThat(fooAnnotationValues.get(0).name()).isEqualTo("value");
    assertThat(fooAnnotationValues.get(0).value()).isInstanceOf(Object[].class);
    assertThat(((Object[]) fooAnnotationValues.get(0).value())[0]).isInstanceOf(Symbol.class);

    Symbol.MethodSymbol bar = (Symbol.MethodSymbol) getSymbol(semanticModel, myClass, "bar");
    assertThat(isAnnotatedWith(bar, myAnnotation)).isTrue();
    assertThat(isGloballyAnnotatedWith(bar, myAnnotation)).isTrue();
    // annotation value retrieved from source, on method
    List<SymbolMetadata.AnnotationValue> barAnnotationValues = valuesForGlobalAnnotation(bar, myAnnotation);
    assertThat(barAnnotationValues).hasSize(1);
    assertThat(barAnnotationValues.get(0).name()).isEqualTo("value");
    assertThat(barAnnotationValues.get(0).value()).isInstanceOf(NewArrayTree.class);

    Symbol.MethodSymbol qix = (Symbol.MethodSymbol) getSymbol(semanticModel, "org.sonar.java.resolve.targets.annotations.MyOtherClass", "qix");
    assertThat(isAnnotatedWith(qix, myAnnotation)).isFalse();
    assertThat(isGloballyAnnotatedWith(qix, myAnnotation)).isTrue();
    // annotation value retrieved from source, on class
    assertThat(valuesForGlobalAnnotation(qix, myAnnotation)).isEmpty();

    assertThat(isAnnotatedWith(qix, myOtherAnnotation)).isFalse();
    assertThat(isGloballyAnnotatedWith(qix, myOtherAnnotation)).isFalse();
    assertThat(valuesForGlobalAnnotation(qix, myOtherAnnotation)).isNull();
  }

  @Test
  public void testIsAnnotatedNullable() {
    Symbol foo = getSymbol("foo");
    assertThat(isAnnotatedNullable(foo)).isFalse();

    getSymbols("nullable").forEach(s -> {
      assertThat(isAnnotatedNullable(s)).as(s + " should be recognized as Nullable.").isTrue();
    });
  }

  @Test
  public void testIsAnnotatedNonNull() {
    Symbol foo = getSymbol("foo");
    assertThat(isAnnotatedNonNull(foo)).isFalse();

    getSymbols("nonnull").forEach(s -> {
      assertThat(isAnnotatedNonNull(s)).as(s + " should be recognized as Nonnull.").isTrue();
    });
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

  private static Symbol getSymbol(SemanticModel semanticModel, String owner, String name) {
    return semanticModel.getClassType(owner).symbol().memberSymbols().stream().filter(s -> name.equals(s.name())).findAny().get();
  }

  private static Stream<Symbol.MethodSymbol> getMethods(SemanticModel semanticModel, String owner) {
    return semanticModel.getClassType(owner).symbol().memberSymbols().stream()
      .filter(s -> s.isMethodSymbol())
      .map(Symbol.MethodSymbol.class::cast)
      .filter(s -> !"<init>".equals(s.name()));
  }
}
