import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class TypeSyntax<K, V extends Number & Comparable<V>> {

  @Target(ElementType.TYPE_USE)
  @interface Tagged {
  }

  List<? extends Number> upper;
  List<? super Integer> lower;
  List<?> unbounded;
  Map<K, List<V>> nested;
  List<String>[] arrayOfLists;
  java.util.Set<java.lang.String> qualified;
  @Tagged String annotated;
  List<@Tagged String> annotatedArgument;
  Map.Entry<K, V> entry;

  <T extends Collection<? super V> & Serializable> T bounded(T input) {
    return input;
  }

  <A, B> Map<A, B> pair(A a, B b) {
    return null;
  }

  void multiCatch() {
    try {
      throw new IOException();
    } catch (IOException | RuntimeException e) {
      e.printStackTrace();
    }
  }

  int[] arrayReturn()[] {
    return null;
  }

  void varargs(List<? extends V>... lists) {
  }
}
