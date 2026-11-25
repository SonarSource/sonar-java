import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class GenericEdges<K, V> {

  class Node {
    K key;
    V value;

    V get() {
      return value;
    }
  }

  static class Outer<T> {
    class Inner {
      T held;

      T held() {
        return held;
      }
    }

    Inner make() {
      return new Inner();
    }
  }

  interface Thrower<E extends Exception> {
    void run() throws E;
  }

  static <E extends Exception> void call(Thrower<E> thrower) throws E {
    thrower.run();
  }

  static <T, R> List<R> mapAll(Collection<? extends T> input, Function<? super T, ? extends R> mapper) {
    List<R> result = new ArrayList<>();
    for (T item : input) {
      result.add(mapper.apply(item));
    }
    return result;
  }

  Map<K, Node> nodes = new HashMap<>();

  void edges(List<? extends Number> numbers, List<? super Integer> sink, Map<String, ? extends List<String>> nested, Optional<String> maybe)
    throws IOException {
    Number first = numbers.get(0);
    sink.add(1);
    Object fromSink = sink.get(0);
    int size = nested.get("k").size();
    Class<?> type = first.getClass();
    Class<? extends Number> typed = first.getClass();
    String value = maybe.orElseThrow(IllegalStateException::new);
    String other = maybe.orElseGet(() -> "x");
    call(() -> {
      throw new IOException("io");
    });
    Outer<String> outer = new Outer<>();
    Outer<String>.Inner inner = outer.make();
    String held = inner.held();
    Outer<String>.Inner explicit = outer.new Inner();
    String explicitHeld = explicit.held;
    Node node = nodes.values().iterator().next();
    V got = node.get();
    K key = node.key;
    List<Integer> lengths = mapAll(List.of("a", "bb"), String::length);
    List<String> strings = GenericEdges.<Integer, String>mapAll(lengths, String::valueOf);
    Map<Boolean, List<String>> parts = strings.stream().collect(Collectors.partitioningBy(s -> s.isEmpty()));
    Map<String, Integer> counts = strings.stream().collect(Collectors.toMap(Function.identity(), String::length, Integer::sum));
    Optional<Integer> longest = lengths.stream().max(Comparator.naturalOrder());
    Stream<List<String>> chunks = Stream.of(strings, strings);
    List<String> flat = chunks.flatMap(List::stream).collect(Collectors.toList());
    BiFunction<String, Integer, String> repeat = String::repeat;
    Supplier<List<String>> factory = ArrayList::new;
    Callable<Integer> callable = lengths::size;
    Iterator<? extends Number> iterator = numbers.iterator();
    Comparator<String> anonymous = new Comparator<>() {
      @Override
      public int compare(String left, String right) {
        return left.compareTo(right);
      }
    };
    List<String> raw = new ArrayList();
    System.out.println(size + value + other + held + explicitHeld + got + key + parts + counts + longest + flat + repeat + factory + callable
      + iterator + anonymous + raw + type + typed + fromSink);
  }
}
