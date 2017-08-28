import java.util.*;

class A {

  Map<Object, Object> map;

  void map() {
    map.put("a", "Apple");
    map.put("a", "Banana"); // Noncompliant [[secondary=8]]{{Verify this is the key that was intended; a value has already been saved for it on line 8.}}
  }

  void map2() {
    map.put("a", "Apple");
    f();
    map.put("a", "Banana"); // FN - not consecutive
    if (blah) {
      map.put(3, "test");
      map.put(4, "another");
      map.put(4, "another"); // Noncompliant {{Verify this is the key that was intended; a value has already been saved for it on line 18.}}
    }

    for (int i = 0; i < 10; i++) {
      map.put(i, "test");
      map.put(i, "test"); // Noncompliant {{Verify this is the key that was intended; a value has already been saved for it on line 23.}}
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
    map.put("a", 1); // Noncompliant {{Verify this is the key that was intended; a value has already been saved for it on line 35.}}
    other.put("a", 2); // Noncompliant {{Verify this is the key that was intended; a value has already been saved for it on line 36.}}
    arr[1] = null; // Noncompliant {{Verify this is the index that was intended; a value has already been saved for it on line 34.}}
  }

  int[] ints;

  void arrays() {
    int i;
    ints[i] = 1;
    ints[i] = 2; // Noncompliant {{Verify this is the index that was intended; a value has already been saved for it on line 46.}}
  }

  void marrays(int[][] arr) {
    arr[0][1] = 1;
    arr[0][2] = 1;
    arr[0][1] = 1; // FN - multidimensional arrays are not handled
  }

  void hashMap() {
    HashMap<Object, Object> hashMap = new HashMap<>();
    hashMap.put("a", "Apple");
    hashMap.put("a", "Banana"); // Noncompliant
  }
}
