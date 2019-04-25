package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import java.util.Objects;

@MethodsAreNonnullByDefault
class EType implements Type, Type.ArrayType {
  private final Ctx ast;
  private final ITypeBinding typeBinding;

  /**
   * Use {@link Ctx#type(ITypeBinding)}
   */
  EType(Ctx ast, ITypeBinding typeBinding) {
    this.ast = Objects.requireNonNull(ast);
    this.typeBinding = Objects.requireNonNull(typeBinding);
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    // TODO unsure about erasure, but it allowed to pass GetClassLoaderCheckTest
    return fullyQualifiedName.equals(
      typeBinding.getErasure().getQualifiedName()
    );
  }

  /**
   * TODO cross test with {@link JavaType#isSubtypeOf(String)}
   */
  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return typeBinding.isSubTypeCompatible(Hack.resolveType(ast.ast, fullyQualifiedName));
  }

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
    return typeBinding.isClass();
  }

  @Override
  public boolean isVoid() {
    return "void".equals(typeBinding.getName());
  }

  @Override
  public boolean isPrimitive() {
    return typeBinding.isPrimitive();
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
   * TODO typeBinding.getBinaryName() for ThrowsSeveralCheckedExceptionCheck ?
   *
   * TODO old implementation returns {@code <nulltype>} for null literals
   *
   * @see JavaSymbol.TypeJavaSymbol#getFullyQualifiedName()
   */
  @Override
  public String fullyQualifiedName() {
    return typeBinding.getErasure().getQualifiedName();
  }

  /**
   * TODO old implementation returns {@code <nulltype>} for null literals
   */
  @Override
  public String name() {
    // TODO unsure about erasure, but it allowed to pass ValueBasedObjectUsedForLockCheck
    return typeBinding.getErasure().getName();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return ast.typeSymbol(typeBinding);
  }

  @Override
  public Type erasure() {
    return ast.type(typeBinding.getErasure());
  }

  /**
   * @see JavaType#toString()
   */
  @Override
  public final String toString() {
    return symbol().toString();
  }

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
      throw new IllegalStateException();
    }
    return ast.type(typeBinding.getElementType());
  }
}
