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
import com.google.common.collect.Sets;

import org.fest.assertions.Fail;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class LeastUpperBoundTest {

  private LeastUpperBound leastUpperBound;

  @Before
  public void setUp() {
    ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
    Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), parametrizedTypeCache));
    TypeSubstitutionSolver typeSubstitutionSolver = new TypeSubstitutionSolver(parametrizedTypeCache, symbols);

    leastUpperBound = new LeastUpperBound(typeSubstitutionSolver, parametrizedTypeCache, symbols);
  }

  @Test
  public void lub_of_one_element_is_itself() {
    CompilationUnitTree cut = treeOf("class A<T> { A<String> var; }");
    ClassTree classA = (ClassTree) cut.types().get(0);
    Type varType = ((VariableTree) classA.members().get(0)).type().symbolType();
    Type a = classA.symbol().type();
    assertThat(leastUpperBound.leastUpperBound(Sets.newHashSet(a))).isSameAs(a);
    assertThat(leastUpperBound.leastUpperBound(Sets.newHashSet(varType))).isSameAs(varType);
  }

  @Test
  public void lub_should_fail_if_no_type_provided() {
    try {
      leastUpperBound.leastUpperBound(Sets.<Type>newHashSet());
      Fail.fail("should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void lub_with_shared_supertypes1() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends Exception {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));

    assertThat(lub.is("java.lang.Exception")).isTrue();
  }

  @Test
  public void lub_with_shared_supertypes2() {
    List<Type> typesFromInput = declaredTypes(
      "import java.io.Serializable;",
      "class A extends Exception {}",
      "class B extends Throwable {}",
      "class C implements Serializable {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type c = typesFromInput.get(2);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b, c));

    assertThat(lub.is("java.io.Serializable")).isTrue();
  }

  @Test
  public void lub_with_shared_supertypes3() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));

    assertThat(lub.is("java.lang.Object")).isTrue();
  }

  @Test
  public void lub_with_hierarchy_of_supertypes1() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends A {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));

    assertThat(lub).isSameAs(a);

    lub = leastUpperBound.leastUpperBound(Sets.newHashSet(b, a));

    assertThat(lub).isSameAs(a);
  }

  @Test
  public void lub_with_hierarchy_of_supertypes2() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends Throwable {}",
      "class C extends B {}");
    Type a = typesFromInput.get(0);
    Type c = typesFromInput.get(2);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, c));

    assertThat(lub.is("java.lang.Throwable")).isTrue();
  }

  @Test
  public void lub_approximation_inheritance_and_multiple_bounds() {
    List<Type> typesFromInput = declaredTypes(
      "class A implements I1, I2 {}",
      "class B implements I2, I1 {}",
      "interface I1 {}",
      "interface I2 {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));

    Type i1 = typesFromInput.get(2);
    // should be <I1 & I2>, not only i1 (first interface of first type analyzed)
    assertThat(lub).isSameAs(i1);
  }

  @Test
  public void lub_approximation_with_complexe_inheritance() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception implements I1, I2 {}",
      "class B extends Exception implements I2, I1 {}",
      "interface I1 {}",
      "interface I2 {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));

    // should be <Exception & I1 & I2>
    assertThat(lub.is("java.lang.Exception")).isTrue();
  }

  @Test
  public void lub_select_best_return_first_classes_then_interfaces_ordered_alphabetically() {
    List<Type> typesFromInput = declaredTypes(
      "class A {}",
      "class B {}",
      "interface I1 {}",
      "interface I2 {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type i1 = typesFromInput.get(2);
    Type i2 = typesFromInput.get(3);

    Type best = LeastUpperBound.best(Lists.newArrayList(i1, a, b, i2));
    assertThat(best.is("A")).isTrue();

    best = LeastUpperBound.best(Lists.newArrayList(i2, i1));
    assertThat(best.is("I1")).isTrue();
  }

  @Test
  public void lub_with_unknown_inheritance() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends UnknownException {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));

    assertThat(lub.isUnknown()).isTrue();
  }

  @Test
  public void lub_of_generics() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends Exception implements I1<Exception> {}",
      "interface I1<T> {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);

    Type lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));
    assertThat(lub).isSameAs(a.symbol().superClass());

    typesFromInput = declaredTypes(
      "class A<T> extends java.util.List<T> {}",
      "class B extends A<String> {}");
    a = typesFromInput.get(0);
    b = typesFromInput.get(1);
    lub = leastUpperBound.leastUpperBound(Sets.newHashSet(a, b));
    assertThat(lub).isSameAs(a);
    // FIXME : should be the other way around but we don't care about type parameter in lub for now.
    assertThat(lub).isSameAs(b.symbol().superClass().erasure());
    assertThat(lub).isNotSameAs(b.symbol().superClass());
  }

  @Test
  public void lub_of_generics_with_raw_type() {
    List<Type> typesFromInput = declaredTypes(
      "class Parent<X> {}",
      "class Child<Y> extends Parent<Y> {}",

      "class ChildString extends Child<String> {}",
      "class ChildRaw extends Child {}");
    Type ChildString = typesFromInput.get(2);
    Type ChildRaw = typesFromInput.get(3);

    JavaType lub = (JavaType) leastUpperBound.leastUpperBound(Sets.newHashSet(ChildString, ChildRaw));
    assertThat(lub.isTagged(JavaType.PARAMETERIZED)).isFalse();
    assertThat(lub.is("Child")).isTrue();
  }

  @Test
  public void lub_of_generics_without_loop() {
    List<Type> typesFromInput = declaredTypes(
      "class Parent<X> {}",
      "class Child<Y> extends Parent<Y> {}",

      "class A {}",
      "class B extends A {}",
      "class C extends A {}",

      "class ChildB extends Child<B> {}",
      "class ChildC extends Child<C> {}");
    Type childB = typesFromInput.get(5);
    Type childC = typesFromInput.get(6);

    JavaType lub = (JavaType) leastUpperBound.leastUpperBound(Sets.newHashSet(childB, childC));
    assertThat(lub.isTagged(JavaType.PARAMETERIZED)).isTrue();
    ParametrizedTypeJavaType ptt = (ParametrizedTypeJavaType) lub;
    assertThat(ptt.rawType.is("Child")).isTrue();
    JavaType substitution = ptt.substitution(ptt.typeParameters().get(0));
    assertThat(substitution.isTagged(JavaType.WILDCARD)).isTrue();
    assertThat(((WildCardType) substitution).boundType).isEqualTo(WildCardType.BoundType.EXTENDS);
    assertThat(((WildCardType) substitution).bound.is("A")).isTrue();
  }

  @Test
  public void lub_of_generics_without_loop2() {
    List<Type> typesFromInput = declaredTypes(
      "class Parent<X> {}",
      "class Child<Y> extends Parent<Y> {}",
      "class Other<Z> {}",

      "class A {}",

      "class ChildP extends Parent<Other<? extends A>> {}",
      "class ChildC extends Child<Other<? extends A>> {}");
    Type ChildP = typesFromInput.get(4);
    Type childC = typesFromInput.get(5);

    JavaType lub = (JavaType) leastUpperBound.leastUpperBound(Sets.newHashSet(ChildP, childC));
    assertThat(lub.isTagged(JavaType.PARAMETERIZED)).isTrue();
    ParametrizedTypeJavaType ptt = (ParametrizedTypeJavaType) lub;
    assertThat(ptt.rawType.is("Parent")).isTrue();
    JavaType substitution = ptt.substitution(ptt.typeParameters().get(0));
    assertThat(substitution.isTagged(JavaType.PARAMETERIZED)).isTrue();
    ptt = (ParametrizedTypeJavaType) substitution;
    assertThat(ptt.rawType.is("Other")).isTrue();
    substitution = ptt.substitution(ptt.typeParameters().get(0));
    assertThat(substitution.isTagged(JavaType.WILDCARD)).isTrue();
    assertThat(((WildCardType) substitution).boundType).isEqualTo(WildCardType.BoundType.EXTENDS);
    assertThat(((WildCardType) substitution).bound.is("A")).isTrue();
  }

  @Test
  public void lub_of_generics_infinite_types() {
    List<Type> typesFromInput = declaredTypes(
      "class Parent<X> {}",
      "class Child<Y> extends Parent<Y> {}",

      "class ChildInteger extends Child<Integer> {}",
      "class ChildString extends Child<String> {}");
    Type childInteger = typesFromInput.get(2);
    Type childString = typesFromInput.get(3);

    JavaType lub = (JavaType) leastUpperBound.leastUpperBound(Sets.newHashSet(childInteger, childString));
    assertThat(lub.isTagged(JavaType.PARAMETERIZED)).isTrue();
    ParametrizedTypeJavaType ptt = (ParametrizedTypeJavaType) lub;
    assertThat(ptt.rawType.is("Child")).isTrue();
    JavaType substitution = ptt.substitution(ptt.typeParameters().get(0));
    assertThat(substitution.isTagged(JavaType.WILDCARD)).isTrue();
    assertThat(((WildCardType) substitution).boundType).isEqualTo(WildCardType.BoundType.EXTENDS);
    assertThat(((WildCardType) substitution).bound.isSubtypeOf("java.lang.Comparable")).isTrue();
  }

  @Test
  public void supertypes() {
    Type arrayList = declaredTypes("class MyArrayList extends java.util.ArrayList<String> {}").get(0);
    Set<Type> supertypes = leastUpperBound.supertypes((JavaType) arrayList);

    assertContains(supertypes,
      "MyArrayList",
      // from MyArrayList
      "java.util.ArrayList<java.lang.String>",
      // from ArrayList
      "java.util.AbstractList<java.lang.String>",
      "java.util.RandomAccess",
      "java.lang.Cloneable",
      "java.io.Serializable",
      // from ArrayList and AbstractList
      "java.util.List<java.lang.String>",
      // from AbstractList
      "java.util.AbstractCollection<java.lang.String>",
      // from List and AbstractCollection
      "java.util.Collection<java.lang.String>",
      // from AbstractCollection
      "java.lang.Object",
      // from Collection
      "java.lang.Iterable<java.lang.String>");
  }

  private static void assertContains(Set<Type> supertypes, String... fullyQualifiedNames) {
    Set<String> toCheck = new HashSet<>(Arrays.asList(fullyQualifiedNames));
    assertThat(supertypes.stream().allMatch(t -> match((JavaType) t, toCheck, fullyQualifiedNames))).isTrue();
    assertThat(toCheck).isEmpty();
  }

  private static boolean match(JavaType type, Set<String> toCheck, String... fullyQualifiedNames) {
    for (String fullyQualifiedName : fullyQualifiedNames) {
      String newFullyQualifiedName = fullyQualifiedName;
      String typeArgFullyQualifiedName = null;
      int param = fullyQualifiedName.indexOf('<');
      if (param > 0) {
        newFullyQualifiedName = fullyQualifiedName.substring(0, param);
        typeArgFullyQualifiedName = fullyQualifiedName.substring(param + 1, fullyQualifiedName.length() - 1);
      }
      if (type.is(newFullyQualifiedName)) {
        if (typeArgFullyQualifiedName != null) {
          assertThat(type.isTagged(JavaType.PARAMETERIZED)).isTrue();
          ParametrizedTypeJavaType ptt = (ParametrizedTypeJavaType) type;
          assertThat(ptt.typeSubstitution.substitutedTypes().get(0).is(typeArgFullyQualifiedName)).isTrue();
        }
        toCheck.remove(fullyQualifiedName);
        return true;
      }
    }
    return false;
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
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(builder.toString());
    SemanticModel.createFor(cut, Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    return cut;
  }
}
