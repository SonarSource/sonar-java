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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaTypeTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(new SquidClassLoader(Collections.emptyList()), new ParametrizedTypeCache()));

  @Test
  public void test_order_of_tags() {
    assertThat(JavaType.BYTE).isLessThan(JavaType.CHAR);
    assertThat(JavaType.CHAR).isLessThan(JavaType.SHORT);
    assertThat(JavaType.SHORT).isLessThan(JavaType.INT);
    assertThat(JavaType.INT).isLessThan(JavaType.LONG);
    assertThat(JavaType.LONG).isLessThan(JavaType.FLOAT);
    assertThat(JavaType.FLOAT).isLessThan(JavaType.DOUBLE);
    assertThat(JavaType.DOUBLE).isLessThan(JavaType.BOOLEAN);
    assertThat(JavaType.BOOLEAN).isLessThan(JavaType.VOID);
    assertThat(JavaType.VOID).isLessThan(JavaType.CLASS);
    assertThat(JavaType.CLASS).isLessThan(JavaType.ARRAY);
  }

  @Test
  public void checkTagging() {
    assertThat(new JavaType(JavaType.VOID, null).isTagged(JavaType.VOID)).isTrue();
  }

  @Test
  public void isNumerical_should_return_true_for_numerical_types() {
    assertThat(new JavaType(JavaType.BYTE, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.CHAR, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.SHORT, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.INT, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.LONG, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.FLOAT, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.DOUBLE, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.BOOLEAN, null).isNumerical()).isFalse();
    assertThat(new JavaType(JavaType.VOID, null).isNumerical()).isFalse();
    assertThat(new JavaType(JavaType.CLASS, null).isNumerical()).isFalse();
  }

  @Test
  public void type_is_fully_qualified_name() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeJavaSymbol typeSymbol2 = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", Symbols.rootPackage);
    ArrayJavaType arrayType = new ArrayJavaType(typeSymbol.type, symbols.arrayClass);
    ClassJavaType classType = (ClassJavaType) typeSymbol.type;
    classType.interfaces = new ArrayList<>();
    assertThat(symbols.byteType.is("byte")).isTrue();
    assertThat(symbols.byteType.is("int")).isFalse();
    assertThat(classType.is("org.foo.bar.MyType")).isTrue();
    assertThat(typeSymbol2.type.is("MyType")).isTrue();
    assertThat(classType.is("org.foo.bar.SomeClass")).isFalse();
    assertThat(arrayType.is("org.foo.bar.MyType[]")).isTrue();
    assertThat(arrayType.is("org.foo.bar.MyType")).isFalse();
    assertThat(arrayType.is("org.foo.bar.SomeClass[]")).isFalse();
    assertThat(symbols.nullType.is("org.foo.bar.SomeClass")).isTrue();
    assertThat(Symbols.unknownType.is("org.foo.bar.SomeClass")).isFalse();
  }

  @Test
  public void isPrimitive() {
    assertThat(new JavaType(JavaType.BYTE, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.CHAR, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.SHORT, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.INT, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.LONG, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.FLOAT, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.DOUBLE, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.BOOLEAN, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.VOID, null).isPrimitive()).isFalse();
    assertThat(new JavaType(JavaType.ARRAY, null).isPrimitive()).isFalse();
    assertThat(new JavaType(JavaType.CLASS, null).isPrimitive()).isFalse();


    //Test primitive type
    for (org.sonar.plugins.java.api.semantic.Type.Primitives primitive : org.sonar.plugins.java.api.semantic.Type.Primitives.values()) {
      assertThat(symbols.charType.isPrimitive(primitive)).isEqualTo(primitive.equals(org.sonar.plugins.java.api.semantic.Type.Primitives.CHAR));
    }

  }

  @Test
  public void erasure_of_type_variable() {
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", new JavaSymbol.PackageJavaSymbol("org.foo.bar", null));
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    typeSubstitution.add((TypeVariableJavaType) new JavaSymbol.TypeVariableJavaSymbol("T", typeSymbol).type, typeSymbol.type);
    ParametrizedTypeJavaType parametrizedType = new ParametrizedTypeJavaType(typeSymbol, typeSubstitution, null);

    TypeVariableJavaType typeVariableType = (TypeVariableJavaType) new JavaSymbol.TypeVariableJavaSymbol("X", typeSymbol).type;
    typeVariableType.bounds = Collections.singletonList(parametrizedType);

    assertThat(typeVariableType.erasure()).isNotEqualTo(parametrizedType);
    assertThat(typeVariableType.erasure()).isEqualTo(parametrizedType.erasure());
  }

  @Test
  public void isSubtypeOf() throws Exception {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeVariableJavaSymbol typeVariableSymbol = new JavaSymbol.TypeVariableJavaSymbol("T", typeSymbol);
    ClassJavaType classType = (ClassJavaType) typeSymbol.type;
    TypeVariableJavaType typeVariableType = (TypeVariableJavaType) typeVariableSymbol.type;
    ArrayJavaType arrayType = new ArrayJavaType(typeSymbol.type, symbols.arrayClass);
    typeVariableType.bounds = Lists.newArrayList(symbols.objectType);

    classType.supertype = symbols.objectType;
    classType.interfaces = Lists.newArrayList(symbols.cloneableType);
    assertThat(classType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(classType.isSubtypeOf(symbols.objectType)).isTrue();

    assertThat(classType.isSubtypeOf("org.foo.bar.MyType")).isTrue();
    assertThat(classType.isSubtypeOf(typeSymbol.type)).isTrue();

    assertThat(classType.isSubtypeOf("java.lang.CharSequence")).isFalse();
    assertThat(classType.isSubtypeOf(symbols.stringType)).isFalse();

    assertThat(classType.isSubtypeOf("java.lang.Cloneable")).isTrue();
    assertThat(classType.isSubtypeOf(symbols.cloneableType)).isTrue();
    assertThat(new JavaType(JavaType.BYTE, null).isSubtypeOf("java.lang.Object")).isFalse();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.MyType[]")).isTrue();
    assertThat(arrayType.isSubtypeOf(new ArrayJavaType(typeSymbol.type, symbols.arrayClass))).isTrue();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.MyType")).isFalse();
    assertThat(arrayType.isSubtypeOf(typeSymbol.type)).isFalse();

    assertThat(arrayType.isSubtypeOf("java.lang.Object[]")).isTrue();
    assertThat(arrayType.isSubtypeOf(new ArrayJavaType(symbols.objectType, symbols.arrayClass))).isTrue();

    assertThat(arrayType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(arrayType.isSubtypeOf(symbols.objectType)).isTrue();

    assertThat(symbols.nullType.isSubtypeOf(symbols.objectType)).isTrue();
    assertThat(symbols.nullType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(symbols.objectType.isSubtypeOf(symbols.nullType)).isFalse();

    assertThat(symbols.nullType.isSubtypeOf(arrayType)).isTrue();
    assertThat(arrayType.isSubtypeOf(symbols.nullType)).isFalse();
    assertThat(symbols.nullType.isSubtypeOf(symbols.nullType)).isTrue();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.SomeClass[]")).isFalse();

    assertThat(typeVariableType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(typeVariableType.is("java.lang.Object")).isFalse();
    assertThat(typeVariableType.isSubtypeOf("java.lang.CharSequence")).isFalse();

    assertThat(Symbols.unknownType.is("java.lang.Object")).isFalse();
    assertThat(Symbols.unknownType.isSubtypeOf("java.lang.CharSequence")).isFalse();
    assertThat(Symbols.unknownType.isSubtypeOf(symbols.objectType)).isFalse();
  }

  @Test
  public void direct_super_types() {
    Set<ClassJavaType> objectDirectSuperTypes = symbols.objectType.directSuperTypes();
    assertThat(objectDirectSuperTypes).isEmpty();

    Set<ClassJavaType> integerDirectSuperTypes = symbols.intType.primitiveWrapperType.directSuperTypes();
    assertThat(integerDirectSuperTypes).hasSize(2);
    assertThat(integerDirectSuperTypes.stream().map(st -> st.fullyQualifiedName())).contains("java.lang.Number", "java.lang.Comparable");

    ArrayJavaType arrayType = new ArrayJavaType(symbols.intType, symbols.arrayClass);
    Set<ClassJavaType> arrayDirectSuperTypes = arrayType.directSuperTypes();
    assertThat(arrayDirectSuperTypes).hasSize(3);
    assertThat(arrayDirectSuperTypes.stream().map(st -> st.fullyQualifiedName())).contains("java.lang.Object", "java.lang.Cloneable", "java.io.Serializable");
  }

  @Test
  public void is_primitive_wrapper() {
    for (JavaType wrapper : symbols.boxedTypes.values()) {
      assertThat(wrapper.isPrimitiveWrapper()).isTrue();
    }
    assertThat(symbols.objectType.isPrimitiveWrapper()).isFalse();
    assertThat(symbols.intType.isPrimitiveWrapper()).isFalse();
  }

  @Test
  public void mapping_wrapper_primitive() {
    for (JavaType wrapper : symbols.boxedTypes.values()) {
      assertThat(wrapper.primitiveType()).isNotNull();
      assertThat(wrapper.primitiveWrapperType()).isNull();
    }
    for (JavaType primitive : symbols.boxedTypes.keySet()) {
      assertThat(primitive.primitiveType()).isNull();
      assertThat(primitive.primitiveWrapperType()).isNotNull();
    }
    assertThat(symbols.objectType.primitiveType()).isNull();
    assertThat(symbols.objectType.primitiveWrapperType()).isNull();
  }

  @Test
  public void parametrizedTypeType_methods_tests() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeVariableJavaSymbol typeVariableSymbol = new JavaSymbol.TypeVariableJavaSymbol("E", typeSymbol);
    ClassJavaType classType = (ClassJavaType) typeSymbol.type;
    TypeVariableJavaType typeVariableType = (TypeVariableJavaType) typeVariableSymbol.type;
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    typeSubstitution.add(typeVariableType, classType);

    ParametrizedTypeJavaType ptt = new ParametrizedTypeJavaType(typeSymbol, typeSubstitution, null);
    assertThat(ptt.substitution(typeVariableType)).isEqualTo(classType);
    assertThat(ptt.substitution(new TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol("F", typeSymbol)))).isNull();
    assertThat(ptt.typeParameters()).hasSize(1);
    assertThat(ptt.typeParameters()).contains(typeVariableType);

    ptt = new ParametrizedTypeJavaType(typeSymbol, null, null);
    assertThat(ptt.substitution(typeVariableType)).isNull();
    assertThat(ptt.typeParameters()).isEmpty();

    assertThat(ptt.isClass()).isTrue();
    assertThat(ptt.isParameterized()).isTrue();
    assertThat(ptt.rawType.isClass()).isTrue();
    assertThat(ptt.rawType.isParameterized()).isFalse();
  }

  @Test
  public void fully_qualified_name() throws Exception {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeJavaSymbol rootPackageTypeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType2", symbols.defaultPackage);
    assertThat(typeSymbol.type.fullyQualifiedName()).isEqualTo("org.foo.bar.MyType");
    assertThat(rootPackageTypeSymbol.type.fullyQualifiedName()).isEqualTo("MyType2");
    JavaSymbol.TypeJavaSymbol typeSymbolNested = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "Nested", typeSymbol);
    assertThat(typeSymbolNested.type.fullyQualifiedName()).isEqualTo("org.foo.bar.MyType$Nested");
    JavaSymbol.TypeJavaSymbol typeSymbolAnonymous = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "", typeSymbol);
    assertThat(typeSymbolAnonymous.type.fullyQualifiedName()).isEqualTo("org.foo.bar.MyType$1");
    JavaSymbol.MethodJavaSymbol methodSymbol = new JavaSymbol.MethodJavaSymbol(Flags.PUBLIC, "<init>", typeSymbolNested);
    JavaSymbol.TypeJavaSymbol typeSymbolAnonymous2 = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "", methodSymbol);
    assertThat(typeSymbolAnonymous2.type.fullyQualifiedName()).isEqualTo("org.foo.bar.MyType$Nested$1");
  }

  @Test
  public void test_fully_qualified_name() {
    File bytecodeDir = new File("target/test-classes");
    ClassFullQualifiedNameVerifierVisitor visitor = new ClassFullQualifiedNameVerifierVisitor(bytecodeDir);
    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/FullyQualifiedName.java"),
      new VisitorsBridge(
        Collections.singletonList(visitor),
        Collections.singletonList(bytecodeDir),
        null));
  }

  private static class ClassFullQualifiedNameVerifierVisitor extends SubscriptionVisitor {
    private final File bytecodeDir;

    public ClassFullQualifiedNameVerifierVisitor(File bytecodeDir) {
      this.bytecodeDir = bytecodeDir;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      int line = ((JavaTree) tree).getLine();
      String expectedFullyQualifiedName = ((ClassTree) tree).openBraceToken().trivias().get(0).comment();
      expectedFullyQualifiedName = expectedFullyQualifiedName.substring(3, expectedFullyQualifiedName.length() - 3);
      String classFullyQualifiedName = ((JavaSymbol.TypeJavaSymbol) ((ClassTree) tree).symbol()).getFullyQualifiedName();
      assertThat(classFullyQualifiedName).as("Symbol mismatch for class at line " + line).isEqualTo(expectedFullyQualifiedName);
      // check for .class file to make sure we match the compiler naming
      File expectedDotClassFile = new File(bytecodeDir, classFullyQualifiedName.replace('.', File.separatorChar) + ".class");
      assertThat(expectedDotClassFile).as("Bytecode file not found for class at line " + line).exists();
    }
  }

  @Test
  public void is_class_is_array() throws Exception {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    ArrayJavaType arrayType = new ArrayJavaType(typeSymbol.type, symbols.arrayClass);

    assertThat(typeSymbol.type.isClass()).isTrue();
    assertThat(typeSymbol.type.isArray()).isFalse();
    assertThat(arrayType.isClass()).isFalse();
    assertThat(arrayType.isArray()).isTrue();

  }
}
