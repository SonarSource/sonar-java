import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

class Conditionals<T extends Number> {

  T value;

  Object mix(boolean flag, int i, long l, Integer boxed, Long boxedLong, byte b, short s, char c, double d, String text, T t, Number n,
    ArrayList<String> arrayList, LinkedList<String> linkedList) {
    long a1 = flag ? i : l;
    int a2 = flag ? i : boxed;
    Object a3 = flag ? boxed : boxedLong;
    short a4 = flag ? b : s;
    char a5 = flag ? c : 1;
    int a6 = flag ? 1 : c;
    double a7 = flag ? d : i;
    Integer a8 = flag ? null : i;
    Integer a9 = flag ? i : null;
    String a10 = flag ? null : text;
    String a11 = flag ? text : null;
    Number a12 = flag ? t : n;
    Number a13 = flag ? n : t;
    List<String> a14 = flag ? arrayList : linkedList;
    Collection<String> a15 = flag ? arrayList : List.of();
    Object a16 = flag ? text : i;
    boolean a17 = flag ? flag : !flag;
    Boolean a18 = flag ? Boolean.TRUE : flag;
    Object a19 = flag ? b : c;
    T a20 = flag ? t : value;
    List<String> a21 = flag ? new ArrayList<>() : new LinkedList<>();
    Supplier<String> a22 = flag ? () -> "a" : String::new;
    return a1 + a2 + String.valueOf(a3) + a4 + a5 + a6 + a7 + a8 + a9 + a10 + a11 + a12 + a13 + a14 + a15 + a16 + a17 + a18 + a19 + a20 + a21 + a22;
  }

  Object casts(Object o) {
    Runnable task = (Runnable & Serializable) () -> System.out.println("task");
    Comparator<String> comparator = (Comparator<String> & Serializable) (x, y) -> x.length() - y.length();
    Object intersection = (Serializable & Comparable<String>) o;
    return switch (o.hashCode()) {
      case 0 -> task;
      case 1 -> comparator;
      default -> intersection;
    };
  }

  Number switchTarget(int k, T t) {
    Number result = switch (k) {
      case 0 -> 1;
      case 1 -> 2L;
      default -> t;
    };
    List<? extends Number> numbers = switch (k) {
      case 0 -> List.of(1);
      default -> new ArrayList<Double>();
    };
    return result.intValue() + numbers.size();
  }

  int unary(byte b, short s, char c, Integer boxed) {
    int x = -b + ~s + +c;
    int y = -boxed;
    long z = -1L;
    return x + y + (int) z + (b++) + (--s);
  }
}
