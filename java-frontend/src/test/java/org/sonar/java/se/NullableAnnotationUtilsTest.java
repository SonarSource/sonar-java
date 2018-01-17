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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNullable;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedWith;
import static org.sonar.java.se.NullableAnnotationUtils.isGloballyAnnotatedWith;
import static org.sonar.java.se.NullableAnnotationUtils.valuesForGlobalAnnotation;

public class NullableAnnotationUtilsTest {

  private SemanticModel semanticModel;

  @Before
  public void setUp() {
    semanticModel = SETestUtils.getSemanticModel("src/test/files/se/annotations/NullableAnnotationUtils.java");
  }

  @Test
  public void private_constructor() throws Exception {
    assertThat(Modifier.isFinal(NullableAnnotationUtils.class.getModifiers())).isTrue();
    Constructor<NullableAnnotationUtils> constructor = NullableAnnotationUtils.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
  }

  @Test
  public void testIsAnnotatedWith() {
    Symbol foo = getSymbol("foo");
    assertThat(isAnnotatedWith(foo, "android.support.annotation.MyAnnotation")).isTrue();
    // not resolved
    assertThat(isAnnotatedWith(foo, "org.bar.MyOtherAnnotation")).isFalse();
  }

  @Test
  public void testIsAnnotatedFromHierarchy() {
    Symbol foo = getSymbol("foo");
    assertThat(isGloballyAnnotatedWith((MethodTree) foo.declaration(), "android.support.annotation.MyAnnotation")).isTrue();

    Symbol bar = getSymbol("bar");
    assertThat(isAnnotatedWith(bar, "android.support.annotation.MyAnnotation")).isFalse();
    MethodTree declaration = (MethodTree) bar.declaration();
    assertThat(isGloballyAnnotatedWith(declaration, "android.support.annotation.MyAnnotation")).isTrue();
    assertThat(isGloballyAnnotatedWith(declaration, "org.bar.MyOtherAnnotation")).isFalse();
  }

  @Test
  public void testGetAnnotationValue() {
    String myClass = "org.sonar.java.resolve.targets.annotations.MyClass";
    String myAnnotation = "org.sonar.java.resolve.targets.annotations.MyAnnotation";
    String myOtherAnnotation = "org.bar.MyOtherAnnotation";
    semanticModel = SETestUtils.getSemanticModel("src/test/java/org/sonar/java/resolve/targets/annotations/MyClass.java");

    Symbol foo = getSymbol(myClass, "foo");
    assertThat(isAnnotatedWith(foo, myAnnotation)).isFalse();
    MethodTree fooDeclaration = (MethodTree) foo.declaration();
    assertThat(isGloballyAnnotatedWith(fooDeclaration, myAnnotation)).isTrue();
    // annotation value retrieved from bytecode, on package
    List<SymbolMetadata.AnnotationValue> fooAnnotationValues = valuesForGlobalAnnotation(fooDeclaration, myAnnotation);
    assertThat(fooAnnotationValues).hasSize(1);
    assertThat(fooAnnotationValues.get(0).name()).isEqualTo("value");
    assertThat(fooAnnotationValues.get(0).value()).isInstanceOf(Object[].class);
    assertThat(((Object[]) fooAnnotationValues.get(0).value())[0]).isInstanceOf(Symbol.class);

    Symbol bar = getSymbol(myClass, "bar");
    assertThat(isAnnotatedWith(foo, myAnnotation)).isFalse();
    MethodTree barDeclaration = (MethodTree) bar.declaration();
    assertThat(isGloballyAnnotatedWith(barDeclaration, myAnnotation)).isTrue();
    // annotation value retrieved from source, on method
    List<SymbolMetadata.AnnotationValue> barAnnotationValues = valuesForGlobalAnnotation(barDeclaration, myAnnotation);
    assertThat(barAnnotationValues).hasSize(1);
    assertThat(barAnnotationValues.get(0).name()).isEqualTo("value");
    assertThat(barAnnotationValues.get(0).value()).isInstanceOf(NewArrayTree.class);

    Symbol qix = getSymbol("org.sonar.java.resolve.targets.annotations.MyOtherClass", "qix");
    assertThat(isAnnotatedWith(qix, myAnnotation)).isFalse();
    MethodTree qixDeclaration = (MethodTree) qix.declaration();
    assertThat(isGloballyAnnotatedWith(qixDeclaration, myAnnotation)).isTrue();
    // annotation value retrieved from source, on class
    assertThat(valuesForGlobalAnnotation(qixDeclaration, myAnnotation)).isEmpty();

    assertThat(isAnnotatedWith(qix, myOtherAnnotation)).isFalse();
    assertThat(isGloballyAnnotatedWith(qixDeclaration, myOtherAnnotation)).isFalse();
    assertThat(valuesForGlobalAnnotation(qixDeclaration, myOtherAnnotation)).isEmpty();
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

  private Symbol getSymbol(String name) {
    return getMainType().symbol().memberSymbols().stream().filter(s -> name.equals(s.name())).findAny().get();
  }

  private Stream<Symbol> getSymbols(String nameStartsWith) {
    return getMainType().symbol().memberSymbols().stream().filter(s -> s.name().startsWith(nameStartsWith));
  }

  private Type getMainType() {
    return semanticModel.getClassType("android.support.annotation.A");
  }

  private Symbol getSymbol(String owner, String name) {
    return semanticModel.getClassType(owner).symbol().memberSymbols().stream().filter(s -> name.equals(s.name())).findAny().get();
  }
}
