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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
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
  public void lub_only_works_with_lists_of_at_least_2_elements() {
    List<Type> typesFromInput = declaredTypes("class A extends Exception {}");
    Type a = typesFromInput.get(0);
    try {
      types.leastUpperBound(Lists.newArrayList(a));
      Fail.fail("should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      types.leastUpperBound(Lists.<Type>newArrayList());
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
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b));

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
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b, c));

    assertThat(lub.is("java.io.Serializable")).isTrue();
  }

  @Test
  public void lub_with_shared_supertypes3() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b));

    assertThat(lub.is("java.lang.Object")).isTrue();
  }

  @Test
  public void lub_with_hierarchy_of_supertypes1() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends A {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b));

    assertThat(lub).isSameAs(a);

    lub = types.leastUpperBound(Lists.newArrayList(b, a));

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
    Type lub = types.leastUpperBound(Lists.newArrayList(a, c));

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
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b));

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
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b));

    // should be <Exception & I1 & I2>
    assertThat(lub.is("java.lang.Exception")).isTrue();
  }

  @Test
  public void lub_with_unknown_inheritance() {
    List<Type> typesFromInput = declaredTypes(
      "class A extends Exception {}",
      "class B extends UnknownException {}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    Type lub = types.leastUpperBound(Lists.newArrayList(a, b));

    assertThat(lub.isUnknown()).isTrue();
  }

  @Test
  public void lub_ignores_generics() {
    List<Type> typesFromInput = typesOfVariables(
      "import java.util.List;",
      "class A {",
      "  List<String> a;",
      "  List<String> b;",
      "}");
    Type a = typesFromInput.get(0);
    Type b = typesFromInput.get(1);
    try {
      types.leastUpperBound(Lists.newArrayList(a, b));
      Fail.fail("should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e.getMessage()).isEqualTo("Generics are not handled");
    }
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

  private static List<Type> typesOfVariables(String... lines) {
    CompilationUnitTree tree = treeOf(lines);
    List<Tree> members = ((ClassTree) tree.types().get(0)).members();
    CollectionUtils.filter(members, new Predicate() {
      @Override
      public boolean evaluate(Object object) {
        Tree tree = (Tree) object;
        return tree.is(Tree.Kind.VARIABLE);
      }
    });
    List<Type> results = Lists.newLinkedList();
    for (Tree member : members) {
      Type type = ((VariableTree) member).type().symbolType();
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
