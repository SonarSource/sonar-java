package checks;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

class IdentityHashMapBoxedKeyCheckSample {

  void boxedKeys(Map<Integer, String> source) {
    Map<Boolean, String> booleans = new IdentityHashMap<>(); // Noncompliant
    Map<Byte, String> bytes = new IdentityHashMap<>(); // Noncompliant
    Map<Character, String> characters = new IdentityHashMap<>(); // Noncompliant
    Map<Short, String> shorts = new IdentityHashMap<>(); // Noncompliant
    Map<Integer, String> integers = new IdentityHashMap<>(); // Noncompliant
    Map<Long, String> longs = new IdentityHashMap<>(); // Noncompliant
    Map<Float, String> floats = new IdentityHashMap<>(); // Noncompliant
    Map<Double, String> doubles = new IdentityHashMap<>(); // Noncompliant
    Map<Integer, String> explicit = new IdentityHashMap<Integer, String>(); // Noncompliant
    Map<Integer, String> copied = new IdentityHashMap<>(source); // Noncompliant
    var inferred = new IdentityHashMap<Integer, String>(); // Noncompliant {{Use a map that compares keys by value because IdentityHashMap compares keys by reference.}}
//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void compliant() {
    Map<String, Boolean> boxedValue = new IdentityHashMap<>();
    Map<String, String> strings = new IdentityHashMap<>();
    Map<Object, String> objects = new IdentityHashMap<>();
    Map<Integer, String> hashMap = new HashMap<>();
    IdentityHashMap raw = new IdentityHashMap();
  }

  <T> void genericKey() {
    Map<T, String> generic = new IdentityHashMap<>();
  }
}
