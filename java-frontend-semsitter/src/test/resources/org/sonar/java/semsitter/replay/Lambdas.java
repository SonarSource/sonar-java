import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class Lambdas {

  Runnable runnable = () -> {
  };
  Supplier<String> supplier = () -> "value";
  Function<String, Integer> length = String::length;
  Function<Integer, int[]> arrays = int[]::new;
  Supplier<StringBuilder> builder = StringBuilder::new;
  BiFunction<Integer, Integer, Integer> sum = (x, y) -> x + y;
  BiFunction<Integer, Integer, Integer> typed = (Integer x, Integer y) -> {
    return x * y;
  };
  Predicate<String> empty = String::isEmpty;
  Predicate<String> notEmpty = empty.negate();
  Function<Object, String> printer = Object::toString;
  Comparator<String> byLength = Comparator.comparing(String::length);

  List<Integer> lengths(List<String> words) {
    return words.stream().map(w -> w.length()).filter(n -> n > 2).collect(Collectors.toList());
  }

  Map<Integer, List<String>> grouped(List<String> words) {
    return words.stream().collect(Collectors.groupingBy(String::length));
  }

  Optional<String> firstUpper(List<String> words) {
    return words.stream().map(String::toUpperCase).findFirst();
  }

  int total(List<String> words) {
    return words.stream().mapToInt(String::length).sum();
  }

  Supplier<String> capture(String prefix) {
    int count = 3;
    return () -> prefix + count + this.hashCode();
  }

  void sort(List<String> words) {
    words.sort((a, b) -> a.compareToIgnoreCase(b));
    words.forEach(System.out::println);
    words.removeIf(word -> word.startsWith("x"));
  }
}
