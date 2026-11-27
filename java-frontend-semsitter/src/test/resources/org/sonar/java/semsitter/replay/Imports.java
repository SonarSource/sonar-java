import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.*;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.Collections.*;
import static java.util.Map.entry;

class Imports implements Comparable<Imports> {

  double circle = 2 * PI;
  int maximum = MAX_VALUE;
  TimeUnit unit = SECONDS;
  Entry<String, Integer> pair = entry("a", 1);
  Map.Entry<String, Integer> qualifiedPair = Map.entry("b", 2);
  java.util.Map.Entry<String, Integer> fullyQualified = pair;
  List<String> sorted = emptyList();
  Set<String> fromStar = new HashSet<>();
  Optional<String> optional = Optional.empty();

  int absolute(int value) {
    return abs(value) + Math.abs(-value);
  }

  void entries(Map<String, List<Integer>> map) {
    for (Entry<String, List<Integer>> entry : map.entrySet()) {
      entry.getKey().length();
      entry.getValue().size();
    }
    for (Map.Entry<String, List<Integer>> entry : new HashMap<>(map).entrySet()) {
      entry.setValue(unmodifiableList(entry.getValue()));
    }
  }

  @Override
  public int compareTo(Imports other) {
    return Integer.compare(maximum, other.maximum);
  }

  interface Greeter {
    default String greet() {
      return "hello";
    }
  }

  interface Polite extends Greeter {
    @Override
    default String greet() {
      return "please " + Greeter.super.greet();
    }
  }

  class Child extends Imports implements Polite {
    @Override
    public String greet() {
      return Polite.super.greet() + Imports.this.maximum + Child.super.toString();
    }

    int parentMaximum() {
      return super.maximum + Imports.this.circle > 0 ? 1 : 0;
    }
  }

  static class Holder {
    static final String CONSTANT = "c";

    static class Deep {
      static int depth = 2;
    }
  }

  String constant = Holder.CONSTANT + Holder.Deep.depth + Imports.Holder.Deep.depth;
}
