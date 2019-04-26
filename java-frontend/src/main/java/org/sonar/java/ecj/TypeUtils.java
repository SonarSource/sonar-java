package org.sonar.java.ecj;

import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Type;

public final class TypeUtils {

  private TypeUtils() {
  }

  /**
   * {@link JavaType#isTagged(int)} {@link JavaType#BOT}
   */
  public static boolean isNullType(Type type) {
    return type.is("null");
  }

  /**
   * {@link JavaType#isTagged(int)} {@link JavaType#TYPEVAR}
   */
  public static boolean isTypeVar(Type type) {
    return ((EType) type).typeBinding.isTypeVariable();
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
   * {@link JavaType#isParameterized()}
   */
  public static boolean isParameterized(Type type) {
    throw new UnsupportedOperationException("isParameterized for " + type);
  }

}
