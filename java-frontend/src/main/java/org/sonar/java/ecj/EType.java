package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.ArrayJavaType;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.java.resolve.TypeVariableJavaType;
import org.sonar.java.resolve.WildCardType;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import java.util.Objects;

@MethodsAreNonnullByDefault
final class EType implements Type, Type.ArrayType {
  final Ctx ast;
  final ITypeBinding typeBinding;

  /**
   * Use {@link Ctx#type(ITypeBinding)}
   */
  EType(Ctx ast, ITypeBinding typeBinding) {
    this.ast = Objects.requireNonNull(ast);
    this.typeBinding = Objects.requireNonNull(typeBinding);
  }

  /**
   * TODO check {@link JavaType#is(String)}
   * TODO check {@link ClassJavaType#is(String)}
   * TODO check {@link ArrayJavaType#is(String)}
   */
  @Override
  public boolean is(String fullyQualifiedName) {
    if (typeBinding.isNullType()) {
      // as in our implementation
      return true;
    }
    return fullyQualifiedName.equals(fullyQualifiedName());
  }

  /**
   * TODO check {@link JavaType#isSubtypeOf(String)}
   * TODO check {@link ClassJavaType#isSubtypeOf(String)}
   * TODO check {@link ArrayJavaType#isSubtypeOf(String)}
   * TODO check {@link TypeVariableJavaType#isSubtypeOf(String)}
   * TODO check {@link WildCardType#isSubtypeOf(String)}
   */
  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return typeBinding.isSubTypeCompatible(Hack.resolveType(ast.ast, fullyQualifiedName));
  }

  /**
   * TODO check {@link JavaType#isSubtypeOf(Type)}
   * TODO check {@link ClassJavaType#isSubtypeOf(Type)}
   * TODO check {@link ArrayJavaType#isSubtypeOf(Type)}
   * TODO check {@link TypeVariableJavaType#isSubtypeOf(Type)}
   * TODO check {@link WildCardType#isSubtypeOf(Type)}
   */
  @Override
  public boolean isSubtypeOf(Type superType) {
    return isSubtypeOf(superType.fullyQualifiedName());
  }

  @Override
  public boolean isArray() {
    return typeBinding.isArray();
  }

  @Override
  public boolean isClass() {
    return typeBinding.isClass()
      // in our implementation also
      || typeBinding.isEnum()
      || typeBinding.isInterface()
      || typeBinding.isAnnotation(); // TODO isAnnotation redundant with isInterface
  }

  @Override
  public boolean isVoid() {
    return "void".equals(typeBinding.getName());
  }

  @Override
  public boolean isPrimitive() {
    return typeBinding.isPrimitive()
      // in our implementation also
      && !isVoid();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return isPrimitive() && primitive.name().toLowerCase().equals(typeBinding.getName());
  }

  @Override
  public boolean isUnknown() {
    return typeBinding.isRecovered();
  }

  @Override
  public boolean isNumerical() {
    // Godin: suboptimal
    return isPrimitive(Primitives.BYTE)
      || isPrimitive(Primitives.CHAR)
      || isPrimitive(Primitives.SHORT)
      || isPrimitive(Primitives.INT)
      || isPrimitive(Primitives.LONG)
      || isPrimitive(Primitives.FLOAT)
      || isPrimitive(Primitives.DOUBLE);
  }

  /**
   * @see JavaSymbol.TypeJavaSymbol#getFullyQualifiedName()
   */
  @Override
  public String fullyQualifiedName() {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    }
    if (typeBinding.isTypeVariable()) {
      return typeBinding.getQualifiedName();
    }
    if (typeBinding.isMember()) {
      // TODO helped for ThrowsSeveralCheckedExceptionCheck and others, add test
      return typeBinding.getBinaryName();
//      return typeBinding.getDeclaringClass().getErasure().getQualifiedName() + "$" + typeBinding.getErasure().getName();
    }
    return typeBinding.getErasure().getQualifiedName();
  }

  @Override
  public String name() {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    }
    if (typeBinding.isTypeVariable()) {
      return typeBinding.getName();
    }
    return typeBinding.getErasure().getName();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return ast.typeSymbol(typeBinding);
  }

  /**
   * TODO check
   */
  @Override
  public Type erasure() {
    return ast.type(typeBinding.getErasure());
  }

  /**
   * TODO check {@link JavaType#toString()}
   * TODO check {@link ArrayJavaType#toString()}
   * TODO check {@link MethodJavaType#toString()}
   */
  @Override
  public final String toString() {
    return symbol().toString();
  }

  /**
   * TODO check {@link ArrayJavaType#equals(Object)}
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public Type elementType() {
    if (!isArray()) {
      // in our implementation only ArrayJavaType implements this method
      throw new IllegalStateException();
    }
    return ast.type(typeBinding.getComponentType());
  }

}
