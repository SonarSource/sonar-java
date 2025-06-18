package checks.unused;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

public class UnusedCollectionCheckSample {
  int getLengthNonComp(String a, String b, String c) {
    List<String> strings = new ArrayList<>();  // Noncompliant {{Consume or remove this unused collection}}
    strings.add(a);
    strings.add(b);
    strings.add(c);
    return a.length() + b.length() + c.length();
  }

  int getLengthComp(String a, String b, String c) {
    return a.length() + b.length() + c.length();
  }

  int getLengthCompTested(String a, String b, String c) {
    List<String> strings = new ArrayList<>();
    strings.add(a);
    strings.add(b);
    strings.add(c);
    return strings.isEmpty() ? -1 : a.length() + b.length() + c.length();
  }

  int getLengthCompForeach(String a, String b, String c) {
    List<String> strings = new ArrayList<>();
    strings.add(a);
    strings.add(b);
    strings.add(c);

    for (String s : strings) {
      System.out.println(s);
    }

    return a.length() + b.length() + c.length();
  }

  int getLengthCompStream(String a, String b, String c) {
    List<String> strings = new ArrayList<>();
    strings.add(a);
    strings.add(b);
    strings.add(c);

    strings.stream().forEach(System.out::println);

    return a.length() + b.length() + c.length();
  }

  int getLengthSuppliedCollection(String a, String b, String c, Supplier<List<String>> supplier) {
    List<String> strings = supplier.get();
    strings.add(a);
    strings.add(b);
    strings.add(c);
    return a.length() + b.length() + c.length();
  }

  int getLengthAddResultUsed(String a, String b, String c) {
    List<String> strings = new ArrayList<>();
    strings.add(a);
    strings.add(b);
    // Add returns value indicating if the collection was changed.
    if(strings.add(c)) {
      return a.length() + b.length() + c.length();
    }
    return -1;
  }

  int getLengthMoreMethods(String a, String b, String c) {
    List<String> strings = new ArrayList<>();  // Noncompliant {{Consume or remove this unused collection}}
  //             ^^^^^^^
    strings.add(a);
    strings.addAll(List.of("x", "y"));
    strings.remove(b);
    strings.removeAll(List.of("z", "w"));
    strings.retainAll(List.of("c", "d"));
    strings.removeIf(s -> s.length() == 3);
    strings.clear();

    return a.length() + b.length() + c.length();
  }

  int unrelated(String a, String b, String c) {
    StringBuilder strings = new StringBuilder();
    strings.append(a);
    strings.append(b);
    strings.append(c);
    return a.length() + b.length() + c.length();
  }

  static class LengthAccumulator {
    int total;
    // FN: we do not consider fields, but ideally we would like to support them as well.
    List<String> strings;

    public LengthAccumulator() {
      this.total = 0;
      this.strings = new ArrayList<>();
    }

    public void add(String s) {
      total += s.length();
      strings.add(s);
    }

    public int getTotal() {
      return total;
    }
  }

  static class MyContainer<T> {
    T obj;
    int count;

    public MyContainer() {
      this.obj = null;
      this.count = 0;
    }

    public void add(T obj) {
      this.obj = obj;
      this.count++;
    }

    public boolean isEven() {
      return count % 2 == 0;
    }
  }

  public void weird() {
    MyContainer<String> myContainer = new MyContainer<>();
    myContainer.add("a");
    myContainer.add("b");
  }
}
