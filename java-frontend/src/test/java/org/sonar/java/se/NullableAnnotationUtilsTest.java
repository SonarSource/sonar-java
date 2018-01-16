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
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNonNull;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNullable;
import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedWith;
import static org.sonar.java.se.NullableAnnotationUtils.isGloballyAnnotatedWith;

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
    assertThat(isGloballyAnnotatedWith((MethodTree) bar.declaration(), "android.support.annotation.MyAnnotation")).isTrue();
    assertThat(isGloballyAnnotatedWith((MethodTree) bar.declaration(), "org.bar.MyOtherAnnotation")).isFalse();
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
}
