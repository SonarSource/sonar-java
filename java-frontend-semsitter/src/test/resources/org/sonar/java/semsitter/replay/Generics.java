import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class Generics<T> {

  static class Box<E> {
    E value;

    Box(E value) {
      this.value = value;
    }

    E get() {
      return value;
    }

    <R> Box<R> map(Function<? super E, ? extends R> mapper) {
      return new Box<>(mapper.apply(value));
    }
  }

  T field;
  List<T> items = new ArrayList<>();
  Map<String, List<Integer>> map = new HashMap<>();
  Box<String> box = new Box<>("text");
  Box<Integer> mapped = box.map(String::length);
  String unboxed = box.get();
  int size = mapped.get();
  List<String> fixed = Arrays.asList("a", "b");
  List<Integer> empty = Collections.emptyList();
  Optional<String> maybe = Optional.of("x").map(s -> s + "y");
  String orElse = maybe.orElse("none");
  List<? extends Number> numbers = List.of(1, 2.0);
  Number firstNumber = numbers.get(0);

  static <A extends Comparable<A>> A max(A left, A right) {
    return left.compareTo(right) > 0 ? left : right;
  }

  int maxInt = max(1, 2);
  String maxString = max("a", "b");
  T firstItem = items.get(0);
  List<String> copied = new ArrayList<>(fixed);
  Map.Entry<String, List<Integer>> entry = map.entrySet().iterator().next();
  List<Integer> values = entry.getValue();

  <K, V> Map<V, K> invert(Map<K, V> source) {
    Map<V, K> result = new HashMap<>();
    source.forEach((k, v) -> result.put(v, k));
    return result;
  }

  Map<Integer, String> inverted = invert(Map.of("a", 1));
}
