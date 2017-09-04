import java.util.*;

class A {

  Map<Object, Object> map;

  void map() {
    map.put("a", "Apple");
    map.put("a", "Banana"); // Noncompliant [[secondary=8]]{{Verify this is the key that was intended; it was already set before.}}
  }

  void map2() {
    map.put("a", "Apple");
    f();
    map.put("a", "Banana"); // FN - not consecutive
    if (blah) {
      map.put(3, "test");
      map.put(4, "another");
      map.put(4, "another"); // Noncompliant [[secondary=18,20,21]]
      map.put(4, "another");
      map.put(4, "another");
    }

    for (int i = 0; i < 10; i++) {
      map.put(i, "test");
      map.put(i, "test"); // Noncompliant [[secondary=25]]
    }

    for (int i = 0; i < 10; i++) {
      map.put(i++, "test");
      map.put(i++, "test"); // Compliant
    }
  }

  void mix(Map<?,?> other, Object[] arr) {
    arr[1] = null;
    map.put("a", 1);
    other.put("a", 2);
    map.put("a", 1); // Noncompliant [[secondary=37]]
    other.put("a", 2); // Noncompliant [[secondary=38]]
    arr[1] = null; // Noncompliant [[secondary=36]]
  }

  int[] ints;

  void arrays() {
    int i;
    ints[i] = 1;
    ints[i] = 2; // Noncompliant [[secondary=48]] {{Verify this is the index that was intended; it was already set before.}}
  }

  void marrays(int[][] arr) {
    arr[0][1] = 1;
    arr[0][2] = 1;
    arr[0][1] = 1; // FN - multidimensional arrays are not handled
  }

  void hashMap() {
    HashMap<Object, Object> hashMap = new HashMap<>();
    hashMap.put("a", "Apple");
    hashMap.put("a", "Apple"); // Noncompliant [[secondary=60,62]]
    hashMap.put("a", "Banana");
  }

  void rhs(int[] arr, Map<Integer, Integer> map, int i) {
    arr[i] = arr[i] + 1;
    arr[i] = arr[i] + 1; // compliant arr[i] is used on RHS
    arr[i] = 3; // Noncompliant [[secondary=67,69,70]]
    arr[i] = 3;
    arr[i] = i;
    arr[i++] = i; // index is not a symbol
  }

  void unknownSymbols(Map<X<?>, X<?>> x) {
    x.put(UNKNOWN_1, BFO);
    x.put(UNKNOWN_2, BLA);
  }

  int[] fieldArr;

  void assignment_to_non_local_array(int i, int[] arr) {
    i = 42;
    this.fieldArr[i] = 0;
    this.fieldArr[i++] = 0; // index is not a symbol
  }

  class MyMap extends HashMap<Object, Object> {
    void map_put_invoked_as_identifier() {
      put(1, 2);
    }
  }
}
