package checks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class MapKeyNotComparableCheck {
  abstract class B<T> {
    Map<T, Object> nonComparable = new HashMap<>();
  }

  static class MyComparable implements Comparable {
    @Override
    public int compareTo(@NotNull Object o) {
      return 0;
    }
  }

  static class NonComparable {
    @Override
    public String toString() {
      return super.toString();
    }
  }

  static class NonComparable2 {
    @Override
    public String toString() {
      return super.toString();
    }
  }

  abstract class A implements Map<NonComparable, String> { // Noncompliant [[sc=35;ec=48]] {{The key type should implement Comparable.}}
  }




  static class FieldDecl {
    Map noParams = new HashMap();
    Map<NonComparable, Object> nonComparable = new HashMap<>(); // Noncompliant {{The key type should implement Comparable.}}
    Map<MyComparable, Object> comparable = new HashMap<>(); // compliant
    HashMap<NonComparable, Object> nonComparable2 = new HashMap<>(); // Noncompliant [[sc=13;ec=26]] {{The key type should implement Comparable.}}
    Map<NonComparable, Object> nonComparable4 = // Noncompliant {{The key type should implement Comparable.}}
      new HashMap<NonComparable, Object>(); // Noncompliant {{The key type should implement Comparable.}}
    Map<NonComparable, Object> nonComparable3 = new HashMap<>(); // Noncompliant {{The key type should implement Comparable.}}
    List<Map<NonComparable, MyComparable>> nonComparable5 = new LinkedList<>(); // Noncompliant [[sc=14;ec=27]] {{The key type should implement Comparable.}}
  }

  static class MethodDecl {
    Map<NonComparable, Object> buildNonComparableMap() { // Noncompliant [[sc=9;ec=22]] {{The key type should implement Comparable.}}
      return null;
    }

    Map<MyComparable, Object> buildComaparableMap() {
      return null;
    }

    void highFunc(Function<Map<NonComparable, String>, String> f) { // Noncompliant [[sc=32;ec=45]] {{The key type should implement Comparable.}}
    }

    // Should throw twice because NomComparable and NonComparable2 are different
    // Noncompliant@+1 [[sc=9;ec=22]] {{The key type should implement Comparable.}}
    Map<NonComparable, Object> highFunc2(Function<Map<NonComparable2, String>, String> f) { // Noncompliant [[sc=55;ec=69]]
      return null;
    }
  }
}
