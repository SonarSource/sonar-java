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
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TypesTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache()));
  private Types types = new Types();

  @Test
  public void isSubtype() {
    // byte is direct subtype of short
    shouldNotBeSubtype(symbols.byteType, Arrays.asList(symbols.booleanType, symbols.charType));
    shouldBeSubtype(symbols.byteType, Arrays.asList(symbols.byteType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // char is direct subtype of int
    shouldNotBeSubtype(symbols.charType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.shortType));
    shouldBeSubtype(symbols.charType, Arrays.asList(symbols.charType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // short is direct subtype of int
    shouldNotBeSubtype(symbols.shortType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType));
    shouldBeSubtype(symbols.shortType, Arrays.asList(symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // int is direct subtype of long
    shouldNotBeSubtype(symbols.intType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType));
    shouldBeSubtype(symbols.intType, Arrays.asList(symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // long is direct subtype of float
    shouldNotBeSubtype(symbols.longType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType));
    shouldBeSubtype(symbols.longType, Arrays.asList(symbols.longType, symbols.floatType, symbols.doubleType));
    // float is direct subtype of double
    shouldNotBeSubtype(symbols.floatType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType));
    shouldBeSubtype(symbols.floatType, Arrays.asList(symbols.floatType, symbols.doubleType));
    // double
    shouldNotBeSubtype(symbols.doubleType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType));
    shouldBeSubtype(symbols.doubleType, Arrays.asList(symbols.doubleType));
    // boolean
    shouldNotBeSubtype(symbols.booleanType, Arrays.asList(symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    shouldBeSubtype(symbols.booleanType, Arrays.asList(symbols.booleanType));

    // TODO test void

    // null
    JavaType.ArrayJavaType arrayTypeInt = new JavaType.ArrayJavaType(symbols.intType, symbols.arrayClass);
    JavaType.ArrayJavaType arrayTypeShort = new JavaType.ArrayJavaType(symbols.shortType, symbols.arrayClass);
    shouldBeSubtype(arrayTypeShort, Arrays.<JavaType>asList(arrayTypeShort, arrayTypeInt));
    shouldNotBeSubtype(symbols.nullType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    shouldBeSubtype(symbols.nullType, Arrays.asList(symbols.nullType, arrayTypeInt, symbols.objectType));
    shouldBeSubtype(arrayTypeInt, Arrays.asList(symbols.objectType));
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", symbols.defaultPackage);
    JavaType.ClassJavaType classType = (JavaType.ClassJavaType) typeSymbol.type;
    classType.interfaces = Lists.newArrayList();
    JavaSymbol.TypeJavaSymbol subtypeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MySubtype", symbols.defaultPackage);
    JavaType.ClassJavaType subClassType = (JavaType.ClassJavaType) subtypeSymbol.type;
    subClassType.supertype = classType;
    subClassType.interfaces = Lists.newArrayList();
    shouldBeSubtype(subClassType, Arrays.<JavaType>asList(classType, subClassType));

  }

  @Test
  public void array_types_equality() throws Exception {
    JavaType.ArrayJavaType arrayInt= new JavaType.ArrayJavaType(symbols.intType, symbols.arrayClass);
    JavaType.ArrayJavaType arrayInt2= new JavaType.ArrayJavaType(symbols.intType, symbols.arrayClass);
    JavaType.ArrayJavaType arrayBoolean = new JavaType.ArrayJavaType(symbols.booleanType, symbols.arrayClass);
    assertThat(arrayInt.equals(arrayInt2)).isTrue();
    assertThat(arrayInt2.equals(arrayInt)).isTrue();
    assertThat(arrayInt2.equals(arrayBoolean)).isFalse();
    assertThat(arrayInt2.equals(arrayInt2)).isTrue();
    assertThat(arrayInt2.equals(null)).isFalse();
    assertThat(arrayInt2.equals(symbols.charType)).isFalse();

  }

  private void shouldNotBeSubtype(JavaType t, List<JavaType> s) {
    for (JavaType type : s) {
      assertThat(types.isSubtype(t, type)).as(t + " is subtype of " + type).isFalse();
    }
  }

  private void shouldBeSubtype(JavaType t, List<JavaType> s) {
    for (JavaType type : s) {
      assertThat(types.isSubtype(t, type)).as(t + " is subtype of " + type).isTrue();
    }
  }

  @Test
  public void lub_of_one_element_is_itself() {
    CompilationUnitTree cut = treeOf("class A<T> { A<String> var; }");
    ClassTree classA = (ClassTree) cut.types().get(0);
    Type varType = ((VariableTree) classA.members().get(0)).type().symbolType();
    Type a = classA.symbol().type();
    assertThat(Types.leastUpperBound(Sets.newHashSet(a))).isSameAs(a);
    assertThat(Types.leastUpperBound(Sets.newHashSet(varType))).isSameAs(varType);
  }

  @Test
  public void lub_should_fail_if_no_type_provided() {
    try {
      Types.leastUpperBound(Sets.<Type>newHashSet());
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
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));

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
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b, c));

    assertThat(lub.is("java.io.Serializable")).isTrue();
  }

  @Test
  public void lub_with_shared_supertypes3() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));

    assertThat(lub.is("java.lang.Object")).isTrue();
  }

  @Test
  public void lub_with_hierarchy_of_supertypes1() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends A {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));

    assertThat(lub).isSameAs(a);

    lub = Types.leastUpperBound(Sets.newHashSet(b, a));

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
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, c));

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
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));

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
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));

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

    Type best = Types.best(Lists.newArrayList(i1, a, b, i2));
    assertThat(best.is("A")).isTrue();

    best = Types.best(Lists.newArrayList(i2, i1));
    assertThat(best.is("I1")).isTrue();
  }

  @Test
  public void lub_with_unknown_inheritance() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends UnknownException {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));

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

    Type lub = Types.leastUpperBound(Sets.newHashSet(a, b));
    assertThat(lub).isSameAs(a.symbol().superClass());

    typesFromInput = declaredTypes(
      "class A<T> extends java.util.List<T> {}",
      "class B extends A<String> {}");
    a = typesFromInput.get(0);
    b = typesFromInput.get(1);
    lub = Types.leastUpperBound(Sets.newHashSet(a, b));
    assertThat(lub).isSameAs(a);
    // FIXME : should be the other way around but we don't care about type parameter in lub for now.
    assertThat(lub).isSameAs(b.symbol().superClass().erasure());
    assertThat(lub).isNotSameAs(b.symbol().superClass());
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
