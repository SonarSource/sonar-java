package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TypeUtils {

  private TypeUtils() {
  }

  /**
   * {@link JavaType#isTagged(int)} {@link JavaType#BOT}
   */
  public static boolean isNullType(Type type) {
    if (!EcjParser.ENABLED) {
      return ((JavaType) type).isTagged(JavaType.BOT);
    }

    return type.is("null");
  }

  /**
   * {@link JavaType#isTagged(int)} {@link JavaType#TYPEVAR}
   */
  public static boolean isTypeVar(Type type) {
    if (!EcjParser.ENABLED) {
      return ((JavaType) type).isTagged(JavaType.TYPEVAR);
    }

    return ((EType) type).typeBinding.isTypeVariable();
  }

  /**
   * {@link JavaType#isTagged(int)} {@link JavaType#INTERSECTION}
   */
  public static boolean isIntersection(Type type) {
    if (!EcjParser.ENABLED) {
      return ((JavaType) type).isTagged(JavaType.INTERSECTION);
    }

    return (((EType) type).typeBinding).isIntersectionType();
  }

  /**
   * {@link JavaType#isPrimitiveWrapper()}
   */
  public static boolean isPrimitiveWrapper(Type type) {
    return type.is("java.lang.Boolean")
      || type.is("java.lang.Byte")
      || type.is("java.lang.Short")
      || type.is("java.lang.Character")
      || type.is("java.lang.Integer")
      || type.is("java.lang.Long")
      || type.is("java.lang.Float")
      || type.is("java.lang.Double");
  }

  /**
   * {@link JavaType#primitiveType()}
   */
  public static Type primitiveType(Type type) {
    return type.symbol().memberSymbols().stream()
      .filter(m -> "value".equals(m.name()))
      .findFirst()
      .orElseThrow(IllegalStateException::new)
      .type();
  }

  /**
   * {@link JavaType#primitiveWrapperType()}
   */
  public static Type primitiveWrapperType(Type type) {
    if (!EcjParser.ENABLED) {
      return ((JavaType) type).primitiveWrapperType();
    }

    EType t = (EType) type;
    switch (t.typeBinding.getName()) {
      case "boolean":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Boolean"));
      case "byte":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Byte"));
      case "short":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Short"));
      case "char":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Character"));
      case "int":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Integer"));
      case "long":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Long"));
      case "float":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Float"));
      case "double":
        return t.ast.type(t.ast.ast.resolveWellKnownType("java.lang.Double"));
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * {@link ClassJavaType#directSuperTypes()}
   */
  public static Set<Type> directSuperTypes(Type type) {
    if (!EcjParser.ENABLED) {
      return ((JavaType) type).directSuperTypes().stream()
        .map(Type.class::cast)
        .collect(Collectors.toSet());
    }

    EType t = (EType) type;
    return Stream
      .concat(Stream.of(t.typeBinding.getInterfaces()), Stream.of(t.typeBinding.getSuperclass()).filter(Objects::nonNull))
      .map(t.ast::type)
      .collect(Collectors.toSet());
  }

  /**
   * {@link ClassJavaType#superTypes()}
   */
  public static Set<Type> superTypes(Type type) {
    HashSet<Type> result = new HashSet<>();
    for (Type t : directSuperTypes(type)) {
      result.add(t);
      result.addAll(superTypes(t));
    }
    return result;
  }

  /**
   * {@link JavaType#isParameterized()}
   */
  public static boolean isParameterized(Type type) {
    if (!EcjParser.ENABLED) {
      return ((JavaType) type).isParameterized();
    }

    return ((EType) type).typeBinding.isParameterizedType();
  }

  /**
   * TODO seems that result of
   * {@link ParametrizedTypeJavaType#substitution(org.sonar.java.resolve.TypeVariableJavaType)}
   * is {@link ITypeBinding#getTypeArguments()}
   **/
  public static List<Type> typeArguments(Type p) {
    if (!EcjParser.ENABLED) {
      ParametrizedTypeJavaType pt = (ParametrizedTypeJavaType) p;
      return pt.typeParameters().stream()
        .map(pt::substitution)
        .collect(Collectors.toList());
    }

    EType e = (EType) p;
    assert e.typeBinding.isParameterizedType();
    return Arrays.stream(e.typeBinding.getTypeArguments())
      .map(e.ast::type)
      .collect(Collectors.toList());
  }

  /**
   * {@link ParametrizedTypeJavaType#typeParameters()}
   */
  public static List<Type> typeParameters(Type p) {
    assert ((EType) p).typeBinding.isParameterizedType();
    throw new UnsupportedOperationException();
  }
}
