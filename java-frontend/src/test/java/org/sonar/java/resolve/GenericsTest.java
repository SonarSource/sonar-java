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
package org.sonar.java.resolve;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericsTest {
  @Test
  public void test_wildcard_instances() {
    assertTypesAreTheSame("A<?>", "A<?>");
    assertTypesAreTheSame("A<? extends String>", "A<? extends String>");
    assertTypesAreTheSame("A<? super Number>", "A<? super Number>");

    assertTypesAreTheSame("A<? super UnknownType>", "A<? super UnknownType>");
    assertTypesAreTheSame("A<? extends UnknownType>", "A<? extends UnknownType>");

    assertTypesAreNotTheSame("A<?>", "A<? extends Object>");
    assertTypesAreNotTheSame("A<?>", "A<? super String>");
    assertTypesAreNotTheSame("A<?>", "A<? extends String>");
    assertTypesAreNotTheSame("A<? extends String>", "A<? super String>");
  }

  private static void assertTypesAreTheSame(String type1, String type2) {
    List<Type> elementTypes = declaredTypesUsingHierarchy(type1, type2);
    Type t1 = elementTypes.get(0);
    Type t2 = elementTypes.get(1);
    SubtypeAssert.assertThat(t1).isSameAs(t2);
  }

  private static void assertTypesAreNotTheSame(String type1, String type2) {
    List<Type> elementTypes = declaredTypesUsingHierarchy(type1, type2);
    Type t1 = elementTypes.get(0);
    Type t2 = elementTypes.get(1);
    SubtypeAssert.assertThat(t1).isNotSameAs(t2);
  }

  @Test
  public void testUnboundedWildCards() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "A<?>",
      "B<?>",
      "B<Animal> ",
      "B<Cat>",
      "Animal",
      "Cat");

    Type wcAType = elementTypes.get(0);
    Type wcBType = elementTypes.get(1);
    Type animalBType = elementTypes.get(2);
    Type catBType = elementTypes.get(3);
    Type animalType = elementTypes.get(4);
    Type catType = elementTypes.get(5);

    SubtypeAssert.assertThat(catType).isSubtypeOf(animalType);

    SubtypeAssert.assertThat(animalBType).isSubtypeOf(wcBType);
    SubtypeAssert.assertThat(wcBType).isNotSubtypeOf(animalBType);

    SubtypeAssert.assertThat(catBType).isSubtypeOf(wcBType);
    SubtypeAssert.assertThat(wcBType).isNotSubtypeOf(catBType);

    SubtypeAssert.assertThat(animalBType).isNotSubtypeOf(catBType);
    SubtypeAssert.assertThat(catBType).isNotSubtypeOf(animalBType);

    SubtypeAssert.assertThat(animalBType).isNotSubtypeOf(animalType);
    SubtypeAssert.assertThat(animalType).isNotSubtypeOf(animalBType);

    SubtypeAssert.assertThat(wcAType).isNotSubtypeOf(wcBType);
    SubtypeAssert.assertThat(wcBType).isSubtypeOf(wcAType);
    SubtypeAssert.assertThat(wcBType).isSubtypeOf(wcAType);
  }

  @Test
  public void testLowerBoundedWildCards() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "B<?>",
      "B<? super Object>",
      "A<? super Object>",
      "A<? super Animal>",
      "A<? super Cat>",
      "A<Cat>",
      "A<Cat>");

    Type wcBType = elementTypes.get(0);
    Type wcSuperObjectBType = elementTypes.get(1);
    Type wcSuperObjectAType = elementTypes.get(2);
    Type wcSuperAnimalAType = elementTypes.get(3);
    Type wcSuperCatAType = elementTypes.get(4);
    Type catAType = elementTypes.get(5);
    Type catAType2 = elementTypes.get(6);

    SubtypeAssert.assertThat(wcBType).isNotSubtypeOf(wcSuperObjectAType);
    SubtypeAssert.assertThat(wcSuperObjectAType).isNotSubtypeOf(wcBType);

    SubtypeAssert.assertThat(wcBType).isNotSubtypeOf(wcSuperObjectBType);
    SubtypeAssert.assertThat(wcSuperObjectBType).isSubtypeOf(wcBType);

    SubtypeAssert.assertThat(wcSuperObjectAType).isNotSubtypeOf(wcSuperObjectBType);
    SubtypeAssert.assertThat(wcSuperObjectBType).isSubtypeOf(wcSuperObjectAType);

    SubtypeAssert.assertThat(wcSuperObjectBType).isSubtypeOf(wcSuperAnimalAType);
    SubtypeAssert.assertThat(wcSuperAnimalAType).isNotSubtypeOf(wcSuperObjectBType);

    SubtypeAssert.assertThat(wcSuperAnimalAType).isSubtypeOf(wcSuperCatAType);
    SubtypeAssert.assertThat(wcSuperCatAType).isNotSubtypeOf(wcSuperAnimalAType);

    SubtypeAssert.assertThat(catAType).isSubtypeOf(wcSuperCatAType);
    SubtypeAssert.assertThat(wcSuperCatAType).isNotSubtypeOf(catAType);

    SubtypeAssert.assertThat(catAType).isSubtypeOf(catAType2);
    SubtypeAssert.assertThat(catAType2).isSubtypeOf(catAType);
  }

  @Test
  public void testUpperBoundedWildCards() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "B<?>",
      "B<? extends Animal>",
      "B<? extends Cat>",
      "B<? extends Object>",
      "B<Cat>",
      "A<? extends Object>");

    Type wcBType = elementTypes.get(0);
    Type wcExtendsAnimalBType = elementTypes.get(1);
    Type wcExtendsCatBType = elementTypes.get(2);
    Type wcExtendsObjectBType = elementTypes.get(3);
    Type catBType = elementTypes.get(4);
    Type wcExtendsObjectAType = elementTypes.get(5);

    SubtypeAssert.assertThat(wcBType).isSubtypeOf(wcExtendsObjectBType);
    SubtypeAssert.assertThat(wcExtendsObjectBType).isSubtypeOf(wcBType);

    SubtypeAssert.assertThat(wcExtendsAnimalBType).isSubtypeOf(wcExtendsObjectBType);
    SubtypeAssert.assertThat(wcExtendsObjectBType).isNotSubtypeOf(wcExtendsAnimalBType);

    SubtypeAssert.assertThat(wcExtendsCatBType).isSubtypeOf(wcExtendsObjectBType);
    SubtypeAssert.assertThat(wcExtendsObjectBType).isNotSubtypeOf(wcExtendsCatBType);

    SubtypeAssert.assertThat(wcExtendsAnimalBType).isNotSubtypeOf(wcExtendsCatBType);
    SubtypeAssert.assertThat(wcExtendsCatBType).isSubtypeOf(wcExtendsAnimalBType);

    SubtypeAssert.assertThat(wcExtendsObjectAType).isNotSubtypeOf(wcExtendsObjectBType);
    SubtypeAssert.assertThat(wcExtendsObjectBType).isSubtypeOf(wcExtendsObjectAType);

    SubtypeAssert.assertThat(wcExtendsObjectAType).isNotSubtypeOf(wcExtendsObjectBType);
    SubtypeAssert.assertThat(catBType).isSubtypeOf(wcExtendsCatBType);
    SubtypeAssert.assertThat(catBType).isSubtypeOf(wcExtendsAnimalBType);
    SubtypeAssert.assertThat(catBType).isSubtypeOf(wcExtendsObjectAType);
    SubtypeAssert.assertThat(catBType).isSubtypeOf(wcExtendsObjectBType);
  }

  @Test
  public void test_type_hierarchy_with_wildcard_classes() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "A<?>",
      "B<?>",
      "C<?>",
      "B<? extends Animal>",
      "B<? extends Object>",
      "A<? extends Object>",
      "A<? super Animal>",
      "Object");

    Type wcAType = elementTypes.get(0);
    Type wcBType = elementTypes.get(1);
    Type wcCType = elementTypes.get(2);
    Type wcExtendsAnimalBType = elementTypes.get(3);
    Type wcExtendsObjectBType = elementTypes.get(4);
    Type wcExtendsObjectAType = elementTypes.get(5);
    Type wcSuperAnimalAType = elementTypes.get(6);
    Type objectType = elementTypes.get(7);

    SubtypeAssert.assertThat(wcBType).isSubtypeOf(wcAType);
    SubtypeAssert.assertThat(wcCType).isNotSubtypeOf(wcAType);
    SubtypeAssert.assertThat(wcCType).isNotSubtypeOf(wcBType);

    SubtypeAssert.assertThat(wcBType).isSubtypeOfObject();
    SubtypeAssert.assertThat(wcBType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(objectType).isNotSubtypeOf(wcBType);

    SubtypeAssert.assertThat(wcExtendsAnimalBType).isSubtypeOfObject();
    SubtypeAssert.assertThat(wcExtendsAnimalBType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(objectType).isNotSubtypeOf(wcExtendsAnimalBType);

    SubtypeAssert.assertThat(wcExtendsObjectBType).isSubtypeOfObject();
    SubtypeAssert.assertThat(wcExtendsObjectBType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(objectType).isNotSubtypeOf(wcExtendsObjectBType);

    SubtypeAssert.assertThat(wcExtendsObjectAType).isSubtypeOfObject();
    SubtypeAssert.assertThat(wcExtendsObjectAType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(objectType).isNotSubtypeOf(wcExtendsObjectAType);

    SubtypeAssert.assertThat(wcSuperAnimalAType).isSubtypeOfObject();
    SubtypeAssert.assertThat(wcSuperAnimalAType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(objectType).isNotSubtypeOf(wcSuperAnimalAType);
  }

  @Test
  public void test_type_hierarchy_directly_between_wildcards_and_other_types() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "A<? extends Cat>",
      "A<? super Cat>",
      "A<?>",
      "Object",
      "Animal",
      "Cat",
      "Integer");
    Type wcExtendsCatType = ((ParametrizedTypeJavaType) elementTypes.get(0)).typeSubstitution.substitutedTypes().get(0);
    Type wcSuperCatType = ((ParametrizedTypeJavaType) elementTypes.get(1)).typeSubstitution.substitutedTypes().get(0);
    Type wcUnboundedType = ((ParametrizedTypeJavaType) elementTypes.get(2)).typeSubstitution.substitutedTypes().get(0);
    Type objectType = elementTypes.get(3);
    Type animalType = elementTypes.get(4);
    Type catType = elementTypes.get(5);
    Type integerType = elementTypes.get(6);

    SubtypeAssert.assertThat(wcExtendsCatType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(wcExtendsCatType).isSubtypeOf(catType);
    SubtypeAssert.assertThat(wcExtendsCatType).isSubtypeOf(animalType);
    SubtypeAssert.assertThat(wcExtendsCatType).isSubtypeOf("java.lang.Object");
    SubtypeAssert.assertThat(wcExtendsCatType).isSubtypeOf("org.foo.Cat");
    SubtypeAssert.assertThat(wcExtendsCatType).isSubtypeOf("org.foo.Animal");

    SubtypeAssert.assertThat(wcSuperCatType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(wcSuperCatType).isNotSubtypeOf(catType);
    SubtypeAssert.assertThat(wcSuperCatType).isNotSubtypeOf(animalType);
    SubtypeAssert.assertThat(wcSuperCatType).isSubtypeOf("java.lang.Object");
    SubtypeAssert.assertThat(wcSuperCatType).isNotSubtypeOf("org.foo.Cat");
    SubtypeAssert.assertThat(wcSuperCatType).isNotSubtypeOf("org.foo.Animal");

    SubtypeAssert.assertThat(wcUnboundedType).isSubtypeOf(objectType);
    SubtypeAssert.assertThat(wcUnboundedType).isNotSubtypeOf(catType);
    SubtypeAssert.assertThat(wcUnboundedType).isNotSubtypeOf(animalType);
    SubtypeAssert.assertThat(wcUnboundedType).isSubtypeOf("java.lang.Object");
    SubtypeAssert.assertThat(wcUnboundedType).isNotSubtypeOf("org.foo.Cat");
    SubtypeAssert.assertThat(wcUnboundedType).isNotSubtypeOf("org.foo.Animal");

    SubtypeAssert.assertThat(wcExtendsCatType).isNotSubtypeOf(integerType);
    SubtypeAssert.assertThat(wcExtendsCatType).isNotSubtypeOf("java.lang.Integer");
  }

  @Test
  public void testWildCardWithObjectType() {
    List<Type> elementTypes = declaredTypesOfVariablesFromLastClass(
      "interface Set<T> {}",

      "class Test<X> {",
      "Object object;",
      "Set<? extends X> set;",
      "X element;",
      "}");
    Type objectType = elementTypes.get(0);
    Type setExtendsXType = elementTypes.get(1);
    Type xType = elementTypes.get(2);
    Type wildCardXType = ((ParametrizedTypeJavaType) setExtendsXType).typeSubstitution.substitutedTypes().get(0);

    SubtypeAssert.assertThat(xType).isSubtypeOf(wildCardXType);
    SubtypeAssert.assertThat(objectType).isNotSubtypeOf(wildCardXType);
  }

  @Test
  public void test_variables_types_parameters_and_wildcards() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "A<?>",
      "A<T>",
      "A<U>",
      "Test<?,?,?>",
      "Test<T,U,V>",
      "Test<?,U,V>",
      "Test<?,V,U>",
      "Test<?,?,U>",
      "Test<?,?,U>");

    Type wcAType = elementTypes.get(0);
    Type varTAType = elementTypes.get(1);
    Type varUAType = elementTypes.get(2);
    Type wcTestWcWcWcType = elementTypes.get(3);
    Type varTestTUVType = elementTypes.get(4);
    Type wcTestWcUVType = elementTypes.get(5);
    Type wcTestWcVUType = elementTypes.get(6);
    Type wcTestWcWcUType = elementTypes.get(7);
    Type wcTestWcWcUType2 = elementTypes.get(8);

    SubtypeAssert.assertThat(varTAType).isSubtypeOf(wcAType);
    SubtypeAssert.assertThat(varUAType).isSubtypeOf(wcAType);

    SubtypeAssert.assertThat(varTestTUVType).isSubtypeOf(wcTestWcWcWcType);
    SubtypeAssert.assertThat(wcTestWcUVType).isSubtypeOf(wcTestWcWcWcType);
    SubtypeAssert.assertThat(wcTestWcVUType).isSubtypeOf(wcTestWcWcWcType);

    SubtypeAssert.assertThat(wcTestWcVUType).isNotSubtypeOf(wcTestWcUVType);

    SubtypeAssert.assertThat(wcTestWcWcUType).isSubtypeOf(wcTestWcWcWcType);
    SubtypeAssert.assertThat(wcTestWcWcUType).isSubtypeOf(wcTestWcWcUType2);
  }

  @Test
  public void test_method_resolution_based_on_wildcards() {
    List<Type> elementTypes = declaredTypes(
      "class Animal {}",
      "class Cat extends Animal {}",
      "class Lion extends Cat {}",

      "class A<T> {",
      "  void foo(A<? extends Animal> a) {",
      // call to foo with wildcard
      "    foo(new A<Animal>());",
      "    foo(new A<Cat>());",
      "    foo(new A<Lion>());",
      // call to foo with object
      "    foo(new A<Object>());",
      "  }",

      "  void foo(Object o) {}",

      "  void bar(A<? super Cat> a) {",
      // call to bar with wildcard
      "    bar(new A<Object>());",
      "    bar(new A<Animal>());",
      "    bar(new A<Cat>());",
      // call to bar with object
      "    bar(new A<Lion>());",
      "  }",

      "  void bar(Object o) {}",

      "  void qix(A<?> a) {",
      "    qix(new A<Object>());",
      "    qix(new A<Animal>());",
      "    qix(new A<Cat>());",
      "    qix(new A<Lion>());",
      "  }",

      "  void gul(A<Cat> a) {",
      "    gul(new A<Animal>());",
      "    gul(new A<Cat>());",
      "    gul(new A<Object>());",
      "  }",

      "  void gul(Object o) {}",
      "}");

    JavaType aType = (JavaType) elementTypes.get(3);
    JavaSymbol.MethodJavaSymbol fooWildCard = getMethodSymbol(aType, "foo", 0);
    JavaSymbol.MethodJavaSymbol fooObject = getMethodSymbol(aType, "foo", 1);
    assertThat(fooWildCard.usages()).hasSize(3);
    assertThat(fooObject.usages()).hasSize(1);

    JavaSymbol.MethodJavaSymbol barWildCard = getMethodSymbol(aType, "bar", 0);
    JavaSymbol.MethodJavaSymbol barObject = getMethodSymbol(aType, "bar", 1);
    assertThat(barWildCard.usages()).hasSize(3);
    assertThat(barObject.usages()).hasSize(1);

    JavaSymbol.MethodJavaSymbol qix = getMethodSymbol(aType, "qix");
    assertThat(qix.usages()).hasSize(4);

    JavaSymbol.MethodJavaSymbol gulGenerics = getMethodSymbol(aType, "gul", 0);
    JavaSymbol.MethodJavaSymbol gulObject = getMethodSymbol(aType, "gul", 1);

    assertThat(gulGenerics.usages()).hasSize(1);
    assertThat(gulObject.usages()).hasSize(2);
  }

  @Test
  public void test_method_resolution() {
    List<Type> elementTypes = declaredTypes(
      "import java.util.Arrays;",
      "import java.util.Collection;",
      "class A {",
      "  void foo() {",
      "    bar(Arrays.asList(\"string\"));",
      "  }",
      "  void bar(Collection<String> c) {}",
      "}");

    JavaType aType = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol bar = getMethodSymbol(aType, "bar");
    assertThat(bar.usages()).hasSize(1);
  }

  @Test
  public void test_method_resolution_nested_parametrized_type_with_wildcards() {
    List<Type> elementTypes = declaredTypes(
      "class A<X> {"
        + "  void foo1(A<A<X>> a) {}"
        + "  void foo2(A<A<? extends X>> a) {}"

        + "  A<A<String>> qix1() { return null; }"
        + "  A<A<? extends String>> qix2() { return null; }"

        + "  void bar() {"
        + "    new A<String>().foo1(qix1());"
        + "    new A<String>().foo2(qix2());"
        + "  }"
        + "}");

    JavaType aType = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol foo1 = getMethodSymbol(aType, "foo1");
    assertThat(foo1.usages()).hasSize(1);

    JavaSymbol.MethodJavaSymbol foo2 = getMethodSymbol(aType, "foo2");
    assertThat(foo2.usages()).hasSize(1);
  }

  @Test
  public void test_method_resolution_with_type_substitution() {
    List<Type> elementTypes = declaredTypes(
      "class A<X> {"
        + "  void foo(A<? extends X> a) {}"

        + "  A<String> qix() { return null; }"

        + "  void bar() {"
        + "    new A<String>().foo(qix());"
        + "  }"
        + "}");

    JavaSymbol.MethodJavaSymbol foo = getMethodSymbol((JavaType) elementTypes.get(0), "foo", 0);
    assertThat(foo.usages()).hasSize(1);
  }

  @Test
  public void test_method_resolution_based_on_wildcards_with_nested_generics() {
    List<Type> elementTypes = declaredTypes(
      "interface I<Z> {}",
      "class A<X extends I<? extends X>> {"
        + "   A(X x) {}"
        + "   <Y extends I<? extends Y>> void foo(Y y) { new A<>(y); }"
        + "}");

    JavaSymbol.MethodJavaSymbol constructor = getMethodSymbol((JavaType) elementTypes.get(1), "<init>", 0);
    assertThat(constructor.usages()).hasSize(1);
  }

  private static MethodJavaSymbol getMethodSymbol(JavaType aType, String methodName, int index) {
    return (JavaSymbol.MethodJavaSymbol) aType.symbol.members.lookup(methodName).get(index);
  }

  private static MethodJavaSymbol getMethodSymbol(JavaType aType, String methodName) {
    return getMethodSymbol(aType, methodName, 0);
  }

  @Test
  public void test_wildcard_grid() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "A",
      "A<?>",
      "A<Animal>",
      "A<Cat>",
      "A<? extends Animal>",
      "A<? extends Cat>",
      "A<? super Animal>",
      "A<? super Cat>",
      "A<T>",
      "A<? extends T>",
      "A<? super T>");

    Type a = elementTypes.get(0).erasure();
    Type aWc = elementTypes.get(1);
    Type aAnimal = elementTypes.get(2);
    Type aCat = elementTypes.get(3);
    Type aExtendsAnimal = elementTypes.get(4);
    Type aExtendsCat = elementTypes.get(5);
    Type aSuperAnimal = elementTypes.get(6);
    Type aSuperCat = elementTypes.get(7);
    Type aT = elementTypes.get(8);
    Type aExtendsT = elementTypes.get(9);
    Type aSuperT = elementTypes.get(10);

    SubtypeAssert.assertThat(a)
      .isSubtypeOf(a)
      .isNotSubtypeOf(aWc, aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aSuperCat, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aWc)
      .isSubtypeOf(a, aWc)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aSuperCat, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aAnimal)
      .isSubtypeOf(a, aWc, aAnimal, aExtendsAnimal, aSuperAnimal, aSuperCat)
      .isNotSubtypeOf(aCat, aExtendsCat, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aCat)
      .isSubtypeOf(a, aWc, aCat, aExtendsAnimal, aExtendsCat, aSuperCat)
      .isNotSubtypeOf(aAnimal, aSuperAnimal, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aExtendsAnimal)
      .isSubtypeOf(a, aWc, aExtendsAnimal)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsCat, aSuperAnimal, aSuperCat, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aExtendsCat)
      .isSubtypeOf(a, aWc, aExtendsAnimal, aExtendsCat)
      .isNotSubtypeOf(aAnimal, aCat, aSuperAnimal, aSuperCat, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aSuperAnimal)
      .isSubtypeOf(a, aWc, aSuperAnimal, aSuperCat)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsAnimal, aExtendsCat, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aSuperCat)
      .isSubtypeOf(a, aWc, aSuperCat)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aT, aExtendsT, aSuperT);

    SubtypeAssert.assertThat(aT)
      .isSubtypeOf(a, aWc, aT, aExtendsT, aSuperT)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aSuperCat);

    SubtypeAssert.assertThat(aExtendsT)
      .isSubtypeOf(a, aWc, aExtendsT)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aSuperCat, aT, aSuperT);

    SubtypeAssert.assertThat(aSuperT)
      .isSubtypeOf(a, aWc, aSuperT)
      .isNotSubtypeOf(aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aSuperCat, aT, aExtendsT);
  }

  @Test
  public void testParameterizedTypeHierarchy() {
    List<Type> elementTypes = declaredTypesUsingHierarchy(
      "B<Cat>",
      "A<Animal>");
    SubtypeAssert.assertThat(elementTypes.get(0)).isNotSubtypeOf(elementTypes.get(1));

    elementTypes = declaredTypesUsingHierarchy(
      "B<Cat>",
      "A<Cat>");
    SubtypeAssert.assertThat(elementTypes.get(0)).isSubtypeOf(elementTypes.get(1));
  }

  @Test
  public void testTypeHierarchyWithGenericClasses() {
    List<Type> elementTypes = declaredTypesOfVariablesFromLastClass(
      "interface Predicate<T> {}",
      "class ObjectPredicate implements Predicate<Object> {}",

      "class Test<X> {",
      "  Predicate<X> myPredicate;",
      "  ObjectPredicate objectPredicate;",
      "  Predicate<Object> oPredicate;",
      "}");
    Type xPredicateType = elementTypes.get(0);
    Type objectPredicateType = elementTypes.get(1);
    Type oPredicateType = elementTypes.get(2);

    SubtypeAssert.assertThat(objectPredicateType).isSubtypeOf(oPredicateType);
    SubtypeAssert.assertThat(oPredicateType).isNotSubtypeOf(objectPredicateType);

    SubtypeAssert.assertThat(xPredicateType).isNotSubtypeOf(objectPredicateType);
    SubtypeAssert.assertThat(objectPredicateType).isNotSubtypeOf(xPredicateType);
    Type xPredicateRAWType = elementTypes.get(0).erasure();
    SubtypeAssert.assertThat(xPredicateRAWType).isNotSubtypeOf(xPredicateType);
    SubtypeAssert.assertThat(xPredicateType).isSubtypeOf(xPredicateRAWType);
  }

  @Test
  public void test_method_resolution_for_parametrized_method_with_provided_substitution() {
    JavaType type = (JavaType) declaredTypesFromFile("src/test/files/resolve/ParametrizedMethodsWithProvidedSubstitution.java").get(0);

    methodHasUsagesWithSameTypeAs(type, "f1", 0, "bString", "bb");
    methodHasUsagesWithSameTypeAs(type, "f1", 1, "aType");

    methodHasUsagesWithSameTypeAs(type, "f2", 0, "integer", "string", "aType");
    methodHasUsagesWithSameTypeAs(type, "f2", 1, "aType");

    methodHasUsagesWithSameTypeAs(type, "f3", "integer");
    methodHasUsagesWithSameTypeAs(type, "f4", (String) null);

    Type stringArray = getMethodInvocationType(getMethodSymbol(type, "f4", 0), 0);
    assertThat(stringArray.isArray()).isTrue();
    assertThat(((ArrayJavaType) stringArray).elementType.is("java.lang.String")).isTrue();
    stringArray = getMethodInvocationType(getMethodSymbol(type, "f4", 1), 0);
    assertThat(stringArray.isArray()).isTrue();
    assertThat(((ArrayJavaType) stringArray).elementType.isArray()).isTrue();
    assertThat(((ArrayJavaType) ((ArrayJavaType) stringArray).elementType).elementType.is("java.lang.String")).isTrue();

    methodHasUsagesWithSameTypeAs(type, "f5", "cStringInteger", "cStringInteger", "cAB");
    methodHasUsagesWithSameTypeAs(type, "f6", "wcSuperA");
    methodHasUsagesWithSameTypeAs(type, "f7", "integer");

    methodHasUsagesWithSameTypeAs(type, "f8", 0, "object");
    methodHasUsagesWithSameTypeAs(type, "f8", 1, "bType", "dType");

    methodHasUsagesWithSameTypeAs(type, "f9", 0, "object", "object");
    methodHasUsagesWithSameTypeAs(type, "f9", 1, "dType");

    methodHasUsagesWithSameTypeAs(type, "f10", "integer");
  }

  @Test
  public void test_method_resolution_with_parametrized_methods() {
    JavaType aType = (JavaType) declaredTypesFromFile("src/test/files/resolve/ParametrizedMethodsWithTypeInference.java").get(0);

    methodHasUsagesWithSameTypeAs(aType, "f1", "bString", "bObject");
    // FIXME type is 'T' when T can not be inferred. Should be Object?
    methodHasUsagesWithSameTypeAs(aType, "f2", "integer", null, "object");
    methodHasUsagesWithSameTypeAs(aType, "f3", "integer");

    Type arrayType = getMethodInvocationType(getMethodSymbol(aType, "f4"), 0);
    assertThat(arrayType.isArray()).isTrue();
    assertThat(((ArrayJavaType) arrayType).elementType.is("java.lang.String")).isTrue();

    methodHasUsagesWithSameTypeAs(aType, "f5", "cStringInteger", "cStringInteger");
    methodHasUsagesWithSameTypeAs(aType, "f6", "wcSuperA", null, null);
    methodHasUsagesWithSameTypeAs(aType, "f7", "integer");

    methodHasUsagesWithSameTypeAs(aType, "f8", 0, "object");
    methodHasUsagesWithSameTypeAs(aType, "f8", 1, "bString");

    methodHasUsagesWithSameTypeAs(aType, "f9", 0, "object", "object");
    methodHasUsagesWithSameTypeAs(aType, "f9", 1, "dType");

    methodHasUsagesWithSameTypeAs(aType, "f10", "integer", "number", "aType");

    methodHasUsagesWithSameTypeAs(aType, "f11", "cStringA");
    methodHasUsagesWithSameTypeAs(aType, "f12", "aType");
    methodHasUsagesWithSameTypeAs(aType, "f13", "integer");
    methodHasUsagesWithSameTypeAs(aType, "f14", "bString");
    methodHasUsagesWithSameTypeAs(aType, "f15", "object");

    methodHasUsagesWithSameTypeAs(aType, "f16", "bRawType", "bRawType", "bRawType", "comparable", "bString");

    methodHasUsagesWithSameTypeAs(aType, "f17", "object");
  }

  @Test
  public void test_method_resolution_with_unchecked_conversions() {
    // JLS8 5.1.9 + JLS8 15.12.2.6
    JavaType bType = (JavaType) declaredTypesFromFile("src/test/files/resolve/UncheckedConversion.java").get(0);

    MethodJavaSymbol foo = getMethodSymbol(bType, "foo");
    Type type = getMethodInvocationType(foo, 0);
    assertThat(type.isArray()).isTrue();
    assertThat(((ArrayJavaType) type).elementType.is("java.lang.Object")).isTrue();
    type = getMethodInvocationType(foo, 1);
    assertThat(type.isArray()).isTrue();
    assertThat(((ArrayJavaType) type).elementType.is("org.foo.A")).isTrue();
    type = getMethodInvocationType(foo, 2);
    assertThat(type.isArray()).isTrue();
    assertThat(((ArrayJavaType) type).elementType.is("org.foo.A")).isTrue();

    methodHasUsagesWithSameTypeAs(bType, "bar", "objectType", "aType", "aType");
    methodHasUsagesWithSameTypeAs(bType, "qix", "bRawType", "bAType", "bAType");
    methodHasUsagesWithSameTypeAs(bType, "gul", "bRawType", "bBAType", "bBAType");
    methodHasUsagesWithSameTypeAs(bType, "lot", "aType", "cType", "cType");
  }

  @Test
  public void test_method_resolution_for_parametrized_method_with_provided_cascading_substitution() {
    List<Type> elementTypes = declaredTypes(
      "class Test {"
        + "  <T extends X, X extends A> void foo(T t) {}"
        + "  void foo(Object o) {}"

        + "  void test() {"
        + "    this.<B, A>foo(new B());"
        + "    this.<A, A>foo(new B());"
        + "    this.<A, B>foo(new B());"
        + "  }"
        + "}"
        + "class A {}"
        + "class B extends A {}");

    JavaType type = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(type, "foo", 0);
    assertThat(methodSymbol.usages()).hasSize(2);
    methodSymbol = getMethodSymbol(type, "foo", 1);
    assertThat(methodSymbol.usages()).hasSize(1);
  }

  @Test
  public void test_method_resolution_for_parametrized_method_with_type_variable_inheritance() {
    List<Type> elementTypes = declaredTypes(
      "class Test<T> {"
        + "  <S extends T> void foo(S s) {}"

        + "  void test() {"
        // type substitution provided
        + "    new Test<A>().<A>foo(new A());"
        + "    new Test<A>().<B>foo(new B());"
        // type inference
        + "    new Test<A>().foo(new A());"
        + "    new Test<A>().foo(new B());"
        + "  }"
        + "}"
        + "class A {}"
        + "class B extends A {}");

    JavaType type = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(type, "foo");
    assertThat(methodSymbol.usages()).hasSize(4);

    elementTypes = declaredTypes(
      "class Test<T> {"
        + "  <S extends T> void foo(S s) {}"

        + "  void test() {"
        // does not compile - not resolved
        + " new Test<B>().foo(new A());"
        + " new Test<B>().<A>foo(new A());"
        + "  }"
        + "}"
        + "class A {}"
        + "class B extends A {}");

    type = (JavaType) elementTypes.get(0);
    methodSymbol = getMethodSymbol(type, "foo");
    assertThat(methodSymbol.usages()).hasSize(0);
  }

  @Test
  public void test_array_of_generics() throws Exception {
    List<Type> elementTypes = declaredTypes(
      "class Test<T> {"
        + "  void foo(T[][]... ts) {}"
        + "  void bar(Class<?>... ts) {}"
        + "  void test(Class type) {"
        + "    new Test<A>().foo(new A[12][14]);"
        +"     bar(new Class[]{Class.class});"
        + "  }"
        + "}" +
        "class A{}");

    JavaType type = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(type, "foo");
    assertThat(methodSymbol.usages()).hasSize(1);
    methodSymbol = getMethodSymbol(type, "bar");
    assertThat(methodSymbol.usages()).hasSize(1);
  }

  @Test
  public void test_method_resolution_for_parametrized_method_and_nested_Parametrized_types() {
    List<Type> elementTypes = declaredTypesFromFile("src/test/files/resolve/generics/parametrizedMethodWithTypeInferenceAndTypeInheritance.java");

    JavaType type = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(type, "foo");
    assertThat(methodSymbol.usages()).hasSize(2);

    assertThat(getMethodSymbol(type, "usesInteger").usages()).hasSize(1);
    assertThat(getMethodSymbol(type, "usesString").usages()).hasSize(1);
  }

  @Test
  public void test_method_resolution_for_parametrized_method_with_inference_from_call_site() {
    List<Type> elementTypes = declaredTypes(
      "class A<E> {"
        + "  static <T> A<T> foo() { return new A<T>(); }"

        + "  void tst() {"
        + "    A<String> a = A.foo();"
        + "  }"
        + "}");

    JavaType type = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(type, "foo");
    assertThat(methodSymbol.usages()).hasSize(1);

    Type methodInvocationType = getMethodInvocationType(methodSymbol, 0);
    assertThat(methodInvocationType.erasure().is("A")).isTrue();
    assertThat(methodInvocationType instanceof ParametrizedTypeJavaType).isTrue();
    assertThat(((ParametrizedTypeJavaType) methodInvocationType).typeSubstitution.substitutedTypes().get(0).is("java.lang.String")).isTrue();
  }

  @Test
  public void test_method_resolution_of_parametrized_method_from_parametrized() throws IOException {
    List<Type> elementTypes = declaredTypesFromFile("src/test/files/resolve/GenericMethods.java");

    JavaType aType = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(aType, "cast");
    assertThat(methodSymbol.usages()).hasSize(3);

    JavaType bType = (JavaType) elementTypes.get(3);
    JavaType objectType = (JavaType) bType.symbol.superClass();
    JavaType cType = (JavaType) elementTypes.get(4);
    JavaType i3Type = (JavaType) elementTypes.get(5);

    assertThat(getMethodSymbol(cType, "bar").usages()).hasSize(2);
    assertThat(getMethodSymbol(i3Type, "foo").usages()).hasSize(2);
    assertThat(getMethodSymbol(objectType, "toString").usages()).hasSize(2);
    assertThat(getMethodSymbol(bType, "print").usages()).hasSize(4);
  }

  @Test
  public void test_method_resolution_with_substitution_in_type_hierarchy() {
    List<Type> elementTypes = declaredTypes(
      "class A {"
        + "  <T> T foo(B<T> b) {"
        + "    return null;"
        + "  }"

        + "  Object foo(Object o) {"
        + "    return null;"
        + "  }"

        + "  void tst() {"
        + "    stringType = foo(new C());"
        + "    stringType = foo(new D());"
        + "    objectType = foo(new A());"
        + "    objectType = foo(new E());"
        + "  }"

        + "  String stringType;"
        + "  Object objectType;"
        + "}"

        + "class B<T> {}"
        + "class C extends B<String> {}"
        + "class D extends C {}"
        + "class E extends A {}");

    JavaType type = (JavaType) elementTypes.get(0);
    JavaSymbol.MethodJavaSymbol methodSymbol = getMethodSymbol(type, "foo");
    assertThat(methodSymbol.usages()).hasSize(2);
  }

  @Test
  public void parametrized_method_resolution_with_bounded_type_variable() {
    List<Type> elementTypes = declaredTypesFromFile("src/test/files/resolve/GenericMethodsBoundedTypeVariables.java");

    JavaSymbol.MethodJavaSymbol classMethod = getMethodSymbol((JavaType) elementTypes.get(0), "foo");
    assertThat(classMethod.usages()).hasSize(12);

    JavaSymbol.MethodJavaSymbol interfaceMethod = getMethodSymbol((JavaType) elementTypes.get(1), "bar");
    assertThat(interfaceMethod.usages()).hasSize(12);
  }

  private static void methodHasUsagesWithSameTypeAs(JavaType type, String methodName, String... variables) {
    methodHasUsagesWithSameTypeAs(type, methodName, 0, variables);
  }

  private static void methodHasUsagesWithSameTypeAs(JavaType type, String methodName, int methodIndex, String... variables) {
    JavaSymbol.MethodJavaSymbol method = getMethodSymbol(type, methodName, methodIndex);

    List<IdentifierTree> usages = method.usages();
    assertThat(usages).overridingErrorMessage("Method '" + methodName + "' should have " + variables.length + " reference(s) but has " + usages.size() + ".")
      .hasSize(variables.length);

    for (int i = 0; i < variables.length; i++) {
      String variableName = variables[i];
      if (variableName != null) {
        Type methodInvocationType = getMethodInvocationType(method, i);
        Type variableType = getVariableType(type, variableName);
        assertThat(methodInvocationType).overridingErrorMessage("Type of expression "+methodInvocationType+" should be the same as type of variable '" + variableName + "'.").isSameAs(variableType);
      }
    }
  }

  private static Type getVariableType(JavaType owner, String variableName) {
    return ((JavaSymbol.VariableJavaSymbol) owner.symbol.members.lookup(variableName).get(0)).type();
  }

  private static Type getMethodInvocationType(MethodJavaSymbol method, int usageIndex) {
    Tree current = method.usages().get(usageIndex);
    while (!current.is(Tree.Kind.METHOD_INVOCATION)) {
      current = current.parent();
    }
    return ((MethodInvocationTree) current).symbolType();
  }

  static class SubtypeAssert extends AbstractAssert<SubtypeAssert, Type> {

    public SubtypeAssert(Type type) {
      super(type, SubtypeAssert.class);
    }

    static SubtypeAssert assertThat(Type type) {
      return new SubtypeAssert(type);
    }

    SubtypeAssert isSubtypeOf(Type... types) {
      for (Type type : types) {
        Assertions.assertThat(actual.isSubtypeOf(type)).overridingErrorMessage(isSubtypeOfMsg(type)).isTrue();
      }
      return this;
    }

    SubtypeAssert isNotSubtypeOf(Type... types) {
      for (Type type : types) {
        Assertions.assertThat(actual.isSubtypeOf(type)).overridingErrorMessage(isNotSubtypeOfMsg(type)).isFalse();
      }
      return this;
    }

    SubtypeAssert isNotSubtypeOfObject() {
      Assertions.assertThat(actual.isSubtypeOf("java.lang.Object")).isFalse();
      return this;
    }

    SubtypeAssert isSubtypeOfObject() {
      Assertions.assertThat(actual.isSubtypeOf("java.lang.Object")).isTrue();
      return this;
    }

    SubtypeAssert isSubtypeOf(String fullyQualifiedName) {
      Assertions.assertThat(actual.isSubtypeOf(fullyQualifiedName)).overridingErrorMessage(isSubtypeOfMsg(fullyQualifiedName)).isTrue();
      return this;
    }

    SubtypeAssert isNotSubtypeOf(String fullyQualifiedName) {
      Assertions.assertThat(actual.isSubtypeOf(fullyQualifiedName)).overridingErrorMessage(isNotSubtypeOfMsg(fullyQualifiedName)).isFalse();
      return this;
    }

    SubtypeAssert isSubtypeOf(Type expected) {
      Assertions.assertThat(actual.isSubtypeOf(expected)).overridingErrorMessage(isSubtypeOfMsg(expected)).isTrue();
      return this;
    }

    SubtypeAssert isNotSubtypeOf(Type expected) {
      Assertions.assertThat(actual.isSubtypeOf(expected)).overridingErrorMessage(isNotSubtypeOfMsg(expected)).isFalse();
      return this;
    }

    private String isNotSubtypeOfMsg(Type type) {
      return isNotSubtypeOfMsg(prettyPrint(type));
    }

    private String isNotSubtypeOfMsg(String type) {
      return "'" + prettyPrint(actual) + "' should not be be a subtype of '" + type + "'";
    }

    private String isSubtypeOfMsg(Type type) {
      return isSubtypeOfMsg(prettyPrint(type));
    }

    private String isSubtypeOfMsg(String type) {
      return "'" + prettyPrint(actual) + "' should be a subtype of '" + type + "'";
    }

    private static String prettyPrint(Type actual) {
      String result = actual.toString();
      JavaType javaType = (JavaType) actual;
      if (javaType instanceof ParametrizedTypeJavaType) {
        result += "<" + getParameters((ParametrizedTypeJavaType) javaType) + ">";
      }
      return result;
    }

    private static String getParameters(ParametrizedTypeJavaType javaType) {
      List<JavaType> substitutedTypes = javaType.typeSubstitution.substitutedTypes();
      List<String> names = new ArrayList<>(substitutedTypes.size());
      for (JavaType type : substitutedTypes) {
        names.add(type.toString());
      }
      return StringUtils.join(names, ",");
    }
  }

  /**
   * Used hierarchy:
   * <pre>
   *   interface A&lt;T&gt; {}
   *   interface B&lt;T&gt; extends A&lt;T&gt; {}
   *   interface C&lt;T&gt; {}
   *   class Animal {}
   *   class Cat extends Animal {}
   *   class Test&lt;T, U, V&gt; {}
   * </pre>
   * @param types
   * @return
   */
  private static List<Type> declaredTypesUsingHierarchy(String... types) {
    String[] linesBefore = new String[] {
      "package org.foo;",
      "interface A<T> {}",
      "interface B<T> extends A<T> {}",
      "interface C<T> {}",
      "class Animal {}",
      "class Cat extends Animal {}",
      "class Test<T,U,V> {"};
    String[] linesAfter = new String[] {"}"};
    String[] variables = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      variables[i] = types[i] + " o" + i + ";";
    }
    return declaredTypesOfVariablesFromLastClass((String[]) ArrayUtils.addAll(ArrayUtils.addAll(linesBefore, variables), linesAfter));
  }

  private static List<Type> declaredTypesOfVariablesFromLastClass(String... lines) {
    CompilationUnitTree tree = treeOf(lines);

    List<Tree> declaredClasses = tree.types();
    Tree last = declaredClasses.get(declaredClasses.size() - 1);
    if (!(last instanceof ClassTree)) {
      return Collections.emptyList();
    }
    ClassTree testClass = (ClassTree) last;
    List<Type> types = new ArrayList<>();
    for (Tree member : testClass.members()) {
      if (member instanceof VariableTree) {
        types.add(((VariableTree) member).type().symbolType());
      }
    }
    return types;
  }

  private static List<Type> declaredTypes(String... lines) {
    CompilationUnitTree tree = treeOf(lines);
    List<Type> results = Lists.newLinkedList();
    for (Tree classTree : tree.types()) {
      Type type = ((ClassTree) classTree).symbol().type();
      results.add(type);
    }
    return results;
  }

  private static CompilationUnitTree treeOf(String... lines) {
    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      builder.append(line).append(System.lineSeparator());
    }
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(builder.toString());
    SemanticModel.createFor(cut, new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes"))));
    return cut;
  }

  private static List<Type> declaredTypesFromFile(String path) {
    CompilationUnitTree tree = treeOf(new File(path));
    List<Type> results = Lists.newLinkedList();
    for (Tree classTree : tree.types()) {
      Type type = ((ClassTree) classTree).symbol().type();
      results.add(type);
    }
    return results;
  }

  private static CompilationUnitTree treeOf(File file) {
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(cut, new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes"))));
    return cut;
  }
}
