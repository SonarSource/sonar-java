package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.ArrayJavaType;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link EType}.
 *
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html
 * <ul>
 *   <li>primitive types - {@link ITypeBinding#isPrimitive()}</li>
 *   <li>reference types<ul>
 *     <li>class types - {@link ITypeBinding#isClass()}</li>
 *     <li>interface types - {@link ITypeBinding#isInterface()}</li>
 *     <li>type variables - {@link ITypeBinding#isTypeVariable()}</li>
 *     <li>array types - {@link ITypeBinding#isArray()}</li>
 *   </ul></li>
 *   <li>null type - {@link ITypeBinding#isNullType()}</li>
 * </ul>
 *
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.5
 * {@link ITypeBinding#isGenericType()}
 * {@link ITypeBinding#isRawType()}
 * {@link ITypeBinding#isParameterizedType()}
 * are mutually exclusive.
 *
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html#jls-8.9
 * <blockquote>enum type, a special kind of class type</blockquote>
 * {@link ITypeBinding#isEnum()}
 *
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-9.html#jls-9.6
 * <blockquote>annotation type, a special kind of interface type</blockquote>
 * {@link ITypeBinding#isAnnotation()}
 */
public class ETypeTest {
  private EType e;
  private JavaType our;

  @Test
  public void class_type() {
    targetIs("package p; class C { }");

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      .isEqualTo(e.typeBinding.isClass());

    assertThat(e.name())
      .isEqualTo("C")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("p.C")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void interface_type() {
    targetIs("package p; interface I { }");

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      // even if
      .isNotEqualTo(e.typeBinding.isClass())
      .isEqualTo(e.typeBinding.isInterface());

    assertThat(e.name())
      .isEqualTo("I")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("p.I")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void enum_type() {
    targetIs("package p; enum E { }");

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      // even if
      .isNotEqualTo(e.typeBinding.isClass())
      .isEqualTo(e.typeBinding.isEnum());

    assertThat(e.name())
      .isEqualTo("E")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("p.E")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void annotation_type() {
    targetIs("package p; @interface A { }");

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      // even if
      .isNotEqualTo(e.typeBinding.isClass())
      .isEqualTo(e.typeBinding.isInterface())
      .isEqualTo(e.typeBinding.isAnnotation());

    assertThat(e.name())
      .isEqualTo("A")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("p.A")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void generic_class_type() {
    targetIs("package p; class C<T> { }");

    assertThat(e.typeBinding.isGenericType())
      .isTrue();

    // TODO
    assertThat(e.typeBinding.isParameterizedType())
      .isFalse();
    assertThat(our.isParameterized())
      .isTrue();

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass());

    assertThat(e.name())
      .isEqualTo("C")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("p.C")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void generic_interface_type() {
    targetIs("package p; interface I<T> { }");

    assertThat(e.typeBinding.isGenericType())
      .isTrue();

    // TODO
    assertThat(e.typeBinding.isParameterizedType())
      .isFalse();
    assertThat(our.isParameterized())
      .isTrue();

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      // even if
      .isNotEqualTo(e.typeBinding.isClass())
      .isEqualTo(e.typeBinding.isInterface());

    assertThat(e.name())
      .isEqualTo("I")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("p.I")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void type_variable() {
    targetIsMethodReturnType("interface C<T> { T m(); }");

    assertThat(e.typeBinding.isTypeVariable()) // TODO used in TypeUtils
      .isTrue();

    assertThat(e.isClass())
      .isFalse()
      .isEqualTo(our.isClass());

    assertThat(e.name())
      .isEqualTo("T")
      .isEqualTo(our.name())
      .isEqualTo(e.typeBinding.getName());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("T")
      .isEqualTo(our.fullyQualifiedName())
      .isEqualTo(e.typeBinding.getQualifiedName());
  }

  @Test
  public void wildcard() {
    targetIsMethodReturnType("interface I { java.util.List<?> m(); }");

    assertThat(e.typeBinding.getTypeArguments()[0].isWildcardType())
      .isTrue();
    // TODO our?
  }

  @Test
  public void parameterized_class_type() {
    targetIsMethodReturnType("interface I { java.util.ArrayList<Object> m(); }");

    assertThat(e.typeBinding.isParameterizedType())
      .isTrue()
      .isEqualTo(our.isParameterized());

    assertThat(e.fullyQualifiedName())
      .isEqualTo("java.util.ArrayList")
      .isEqualTo(our.fullyQualifiedName());
    // even if
    assertThat(e.typeBinding.getQualifiedName())
      .isEqualTo("java.util.ArrayList<java.lang.Object>");

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass());

    assertThat(e.name())
      .isEqualTo("ArrayList")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("java.util.ArrayList")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void parameterized_interface_type() {
    targetIsMethodReturnType("interface I { java.util.List<Object> m(); }");

    assertThat(e.typeBinding.isParameterizedType())
      .isTrue()
      .isEqualTo(our.isParameterized());

    assertThat(e.fullyQualifiedName())
      .isEqualTo("java.util.List")
      .isEqualTo(our.fullyQualifiedName());
    // even if
    assertThat(e.typeBinding.getQualifiedName())
      .isEqualTo("java.util.List<java.lang.Object>");

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      // even if
      .isNotEqualTo(e.typeBinding.isClass())
      .isEqualTo(e.typeBinding.isInterface());

    assertThat(e.name())
      .isEqualTo("List")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("java.util.List")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void raw_interface_type() {
    targetIsMethodReturnType("interface I { java.util.List m(); }");

    assertThat(e.typeBinding.isRawType())
      .isTrue();

    assertThat(e.typeBinding.isParameterizedType())
      .isFalse()
      .isEqualTo(our.isParameterized());

    assertThat(e.isClass())
      .isTrue()
      .isEqualTo(our.isClass())
      // even if
      .isNotEqualTo(e.typeBinding.isClass())
      .isEqualTo(e.typeBinding.isInterface());

    assertThat(e.name())
      .isEqualTo("List")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("java.util.List")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void primitive_type() {
    targetIsMethodReturnType("interface I { int m(); }");

    assertThat(e.isPrimitive())
      .isTrue()
      .isEqualTo(our.isPrimitive())
      .isEqualTo(e.typeBinding.isPrimitive());

    assertThat(e.name())
      .isEqualTo("int")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("int")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void array_type() {
    targetIsMethodReturnType("interface I { int[][] m(); }");

    assertThat(e.isArray())
      .isTrue()
      .isEqualTo(our.isArray())
      .isEqualTo(e.typeBinding.isArray());

    assertThat(e.elementType().isArray())
      .isTrue()
      .isEqualTo(((ArrayJavaType) our).elementType().isArray())
      .isNotEqualTo(e.typeBinding.getElementType().isArray())
      .isEqualTo(e.typeBinding.getComponentType().isArray());

    // TODO
    assertThat(e.name())
      .isEqualTo("int[][]")
      .isEqualTo(e.typeBinding.getName());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("int[][]")
      .isEqualTo(e.typeBinding.getQualifiedName());
    // unlike
    assertThat(our.name())
      .isEqualTo("Array");
    assertThat(our.fullyQualifiedName())
      .isEqualTo("$Array");

    assertThat(e.isSubtypeOf("java.io.Serializable"))
      .isFalse()
      .isEqualTo(our.isSubtypeOf("java.io.Serializable"))
      // even if
      .isNotEqualTo(e.typeBinding.isSubTypeCompatible(Hack.resolveType(e.ast.ast, "java.io.Serializable")));
  }

  @Test
  public void void_type() {
    targetIsMethodReturnType("interface I { void m(); }");

    assertThat(e.isVoid())
      .isTrue()
      .isEqualTo(our.isVoid());

    assertThat(e.isPrimitive())
      .isFalse()
      .isEqualTo(our.isPrimitive())
      // even if
      .isNotEqualTo(e.typeBinding.isPrimitive());

    assertThat(e.name())
      .isEqualTo("void")
      .isEqualTo(our.name());
    assertThat(e.fullyQualifiedName())
      .isEqualTo("void")
      .isEqualTo(our.fullyQualifiedName());
  }

  @Test
  public void null_type() {
    targetIsExpression("class C { Object m() { return null; } }");

    assertThat(e.typeBinding.isNullType())
      .isTrue();

    assertThat(e.isPrimitive())
      .isFalse()
      .isEqualTo(our.isPrimitive())
      .isEqualTo(e.typeBinding.isPrimitive());

    assertThat(e.name())
      .isEqualTo("<nulltype>")
      .isEqualTo(our.name());
    // even if
    assertThat(e.typeBinding.getName())
      .isEqualTo("null");
    // similarly
    assertThat(e.fullyQualifiedName())
      .isEqualTo("<nulltype>")
      .isEqualTo(our.fullyQualifiedName());
    // even if
    assertThat(e.typeBinding.getQualifiedName())
      .isEqualTo("null");

    assertThat(e.is("null")) // TODO used in TypeUtils
      .isTrue();
    // TODO asymmetry with name?
    assertThat(our.is("null"))
      .isTrue();
    // TODO or by design?
    assertThat(our.is("anything"))
      .isTrue();
  }

  @Test
  public void should_be_final_non_public() {
    assertThat(Modifier.isFinal(EType.class.getModifiers()))
      .isTrue();
    assertThat(Modifier.isPublic(EType.class.getModifiers()))
      .isFalse();
  }

  private void targetIs(String source) {
    Function<Function<String, ClassTree>, Type> toType = parser -> parser.apply(source).symbol().type();
    e = (EType) toType.apply(ETypeTest::ecjFrontend);
    our = (JavaType) toType.apply(ETypeTest::ourFrontend);
  }

  private void targetIsMethodReturnType(String source) {
    Function<Function<String, ClassTree>, Type> toType = (parser) -> {
      ClassTree c = parser.apply(source);
      MethodTree m = (MethodTree) c.members().get(0);
      return Objects.requireNonNull(m.returnType()).symbolType();
    };
    e = (EType) toType.apply(ETypeTest::ecjFrontend);
    our = (JavaType) toType.apply(ETypeTest::ourFrontend);
  }

  private void targetIsExpression(String source) {
    Function<Function<String, ClassTree>, Type> toType = parser -> {
      ClassTree c = parser.apply(source);
      MethodTree m = (MethodTree) c.members().get(0);
      ReturnStatementTree r = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
      return Objects.requireNonNull(r.expression()).symbolType();
    };
    e = (EType) toType.apply(ETypeTest::ecjFrontend);
    our = (JavaType) toType.apply(ETypeTest::ourFrontend);
  }

  private static ClassTree ecjFrontend(String source) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) EcjParser.parse(source);
    return (ClassTree) compilationUnit.types().get(0);
  }

  private static ClassTree ourFrontend(String source) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) JavaParser.createParser().parse(source);
    SemanticModel.createFor(compilationUnit, new SquidClassLoader(Collections.emptyList()));
    return (ClassTree) compilationUnit.types().get(0);
  }

}
