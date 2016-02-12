/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.java.resolve.JavaType.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class WildcardsTest {
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

    Type wildCardObjectType = ((ParametrizedTypeJavaType) wcExtendsObjectAType).typeSubstitution.substitutedTypes().get(0);
    SubtypeAssert.assertThat(objectType).isSubtypeOf(wildCardObjectType);
    SubtypeAssert.assertThat(wildCardObjectType).isNotSubtypeOf(objectType);
    SubtypeAssert.assertThat(wildCardObjectType).isNotSubtypeOfObject();
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
      "Test<?,?,U>",
      "Test<?,?>");

    Type wcAType = elementTypes.get(0);
    Type varTAType = elementTypes.get(1);
    Type varUAType = elementTypes.get(2);
    Type wcTestWcWcWcType = elementTypes.get(3);
    Type varTestTUVType = elementTypes.get(4);
    Type wcTestWcUVType = elementTypes.get(5);
    Type wcTestWcVUType = elementTypes.get(6);
    Type wcTestWcWcUType = elementTypes.get(7);
    Type wcTestWcWcUType2 = elementTypes.get(8);
    Type wcTestWcWcType = elementTypes.get(9);

    SubtypeAssert.assertThat(varTAType).isSubtypeOf(wcAType);
    SubtypeAssert.assertThat(varUAType).isSubtypeOf(wcAType);

    SubtypeAssert.assertThat(varTestTUVType).isSubtypeOf(wcTestWcWcWcType);
    SubtypeAssert.assertThat(wcTestWcUVType).isSubtypeOf(wcTestWcWcWcType);
    SubtypeAssert.assertThat(wcTestWcVUType).isSubtypeOf(wcTestWcWcWcType);

    SubtypeAssert.assertThat(wcTestWcVUType).isNotSubtypeOf(wcTestWcUVType);

    SubtypeAssert.assertThat(wcTestWcWcUType).isSubtypeOf(wcTestWcWcWcType);
    SubtypeAssert.assertThat(wcTestWcWcUType).isSubtypeOf(wcTestWcWcUType2);

    SubtypeAssert.assertThat(wcTestWcWcWcType).isNotSubtypeOf(wcTestWcWcType);
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

    JavaSymbol.MethodJavaSymbol qix = getMethodSymbol(aType, "qix", 0);
    assertThat(qix.usages()).hasSize(4);

    JavaSymbol.MethodJavaSymbol gulGenerics = getMethodSymbol(aType, "gul", 0);
    JavaSymbol.MethodJavaSymbol gulObject = getMethodSymbol(aType, "gul", 1);

    // FIXME SONARJAVA-1514 generics should be handled correctly
    assertThat(gulGenerics.usages()).hasSize(3); // should be 1
    assertThat(gulObject.usages()).hasSize(0); // should be 2
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

    JavaSymbol.MethodJavaSymbol foo1 = getMethodSymbol((JavaType) elementTypes.get(0), "foo1", 0);
    assertThat(foo1.usages()).hasSize(1);

    JavaSymbol.MethodJavaSymbol foo2 = getMethodSymbol((JavaType) elementTypes.get(0), "foo2", 0);
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

    Type a = elementTypes.get(0);
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
      .isSubtypeOf(a, aWc, aAnimal, aCat, aExtendsAnimal, aExtendsCat, aSuperAnimal, aSuperCat, aT, aExtendsT, aSuperT);

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
    Type bType = elementTypes.get(0);
    Type aType = elementTypes.get(1);

    // FIXME SONARJAVA-1514
    // SubtypeAssert.assertThat(bType).isSubtypeOf(aType);
  }

  @Test
  public void testTypeHierarchyWithGenericClasses() {
    List<Type> elementTypes = declaredTypesOfVariablesFromLastClass(
      "interface Predicate<T> {}",
      "class ObjectPredicate implements Predicate<Object> {}",

      "class Test<X> {",
      "Predicate<X> myPredicate;",
      "ObjectPredicate objectPredicate;",
      "}");
    Type xPredicateType = elementTypes.get(0);
    Type objectPredicateType = elementTypes.get(1);

    SubtypeAssert.assertThat(xPredicateType).isNotSubtypeOf(objectPredicateType);
    // FIXME SONARJAVA-1514
    SubtypeAssert.assertThat(objectPredicateType).isSubtypeOf(xPredicateType);
  }

  static class SubtypeAssert extends GenericAssert<SubtypeAssert, Type> {

    public SubtypeAssert(Type type) {
      super(SubtypeAssert.class, type);
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

    SubtypeAssert isSubtypeOf(Type expected) {
      Assertions.assertThat(actual.isSubtypeOf(expected)).overridingErrorMessage(isSubtypeOfMsg(expected)).isTrue();
      return this;
    }

    SubtypeAssert isNotSubtypeOf(Type expected) {
      Assertions.assertThat(actual.isSubtypeOf(expected)).overridingErrorMessage(isNotSubtypeOfMsg(expected)).isFalse();
      return this;
    }

    private String isNotSubtypeOfMsg(Type type) {
      return "'" + prettyPrint(actual) + "' should not be be a subtype of '" + prettyPrint(type) + "'";
    }

    private String isSubtypeOfMsg(Type type) {
      return "'" + prettyPrint(actual) + "' should be a subtype of '" + prettyPrint(type) + "'";
    }

    private static String prettyPrint(Type actual) {
      String result = actual.toString();
      JavaType javaType = (JavaType) actual;
      if (javaType instanceof JavaType.ParametrizedTypeJavaType) {
        result += "<" + getParameters((JavaType.ParametrizedTypeJavaType) javaType) + ">";
      }
      return result;
    }

    private static String getParameters(JavaType.ParametrizedTypeJavaType javaType) {
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

  private static CompilationUnitTree treeOf(String... lines) {
    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      builder.append(line).append(System.lineSeparator());
    }
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(builder.toString());
    SemanticModel.createFor(cut, Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    return cut;
  }

  private static List<Type> declaredTypes(String... lines) {
    CompilationUnitTree tree = treeOf(lines);
    List<Type> results = Lists.newLinkedList();
    for (Tree classTree : tree.types()) {
      Type type = ((TypeJavaSymbol) ((ClassTree) classTree).symbol()).type();
      results.add(type);
    }
    return results;
  }
}
