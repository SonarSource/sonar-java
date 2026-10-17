package org.sonar.java.javac.model;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class AnnotationProxySupport {

  private AnnotationProxySupport() {
  }

  static <A extends Annotation> A annotationForMap(Class<A> type, Map<String, Object> values) {
    InvocationHandler handler = (proxy, method, args) -> {
      String name = method.getName();
      if (method.getParameterCount() == 0) {
        if (name.equals("annotationType")) {
          return type;
        }
        if (name.equals("toString")) {
          return toString(type, values);
        }
        if (name.equals("hashCode")) {
          return hashCode(type, values);
        }
        Object value = values.getOrDefault(name, method.getDefaultValue());
        if (value == null) {
          throw new IncompleteAnnotationException(type, name);
        }
        if (value instanceof ExceptionProxy exception) {
          throw exception.generateException();
        }
        return value.getClass().isArray() ? cloneArray(value) : value;
      }
      if (name.equals("equals") && method.getParameterCount() == 1) {
        return equals(type, values, args[0]);
      }
      throw new IllegalArgumentException(method.toString());
    };
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }

  static Class<?> invocationHandlerReturnType(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == boolean.class) return Boolean.class;
    if (type == byte.class) return Byte.class;
    if (type == short.class) return Short.class;
    if (type == int.class) return Integer.class;
    if (type == long.class) return Long.class;
    if (type == char.class) return Character.class;
    if (type == float.class) return Float.class;
    return Double.class;
  }

  private static Object cloneArray(Object value) {
    int length = Array.getLength(value);
    Object copy = Array.newInstance(value.getClass().getComponentType(), length);
    System.arraycopy(value, 0, copy, 0, length);
    return copy;
  }

  private static boolean equals(Class<? extends Annotation> type, Map<String, Object> values, Object other) {
    if (!type.isInstance(other)) {
      return false;
    }
    for (Method method : type.getDeclaredMethods()) {
      Object value = values.getOrDefault(method.getName(), method.getDefaultValue());
      try {
        if (!Objects.deepEquals(value, method.invoke(other))) {
          return false;
        }
      } catch (ReflectiveOperationException e) {
        return false;
      }
    }
    return true;
  }

  private static int hashCode(Class<? extends Annotation> type, Map<String, Object> values) {
    int result = 0;
    for (Method method : type.getDeclaredMethods()) {
      Object value = values.getOrDefault(method.getName(), method.getDefaultValue());
      result += (127 * method.getName().hashCode()) ^ valueHashCode(value);
    }
    return result;
  }

  private static int valueHashCode(Object value) {
    if (value instanceof Object[] objects) return Arrays.deepHashCode(objects);
    if (value instanceof boolean[] array) return Arrays.hashCode(array);
    if (value instanceof byte[] array) return Arrays.hashCode(array);
    if (value instanceof short[] array) return Arrays.hashCode(array);
    if (value instanceof int[] array) return Arrays.hashCode(array);
    if (value instanceof long[] array) return Arrays.hashCode(array);
    if (value instanceof char[] array) return Arrays.hashCode(array);
    if (value instanceof float[] array) return Arrays.hashCode(array);
    if (value instanceof double[] array) return Arrays.hashCode(array);
    return value.hashCode();
  }

  private static String toString(Class<? extends Annotation> type, Map<String, Object> values) {
    String members = Arrays.stream(type.getDeclaredMethods())
      .map(method -> method.getName() + "=" + Objects.toString(values.getOrDefault(method.getName(), method.getDefaultValue())))
      .collect(Collectors.joining(", "));
    return "@" + type.getName() + "(" + members + ")";
  }

  abstract static class ExceptionProxy implements Serializable {
    abstract RuntimeException generateException();
  }

  static final class EnumConstantNotPresentExceptionProxy extends ExceptionProxy {
    private final Class<? extends Enum<?>> type;
    private final String name;

    EnumConstantNotPresentExceptionProxy(Class<? extends Enum<?>> type, String name) {
      this.type = type;
      this.name = name;
    }

    @Override
    RuntimeException generateException() {
      return new EnumConstantNotPresentException(type, name);
    }
  }
}
