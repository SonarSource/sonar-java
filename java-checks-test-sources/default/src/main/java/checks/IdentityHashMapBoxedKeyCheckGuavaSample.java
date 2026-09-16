package checks;

import com.google.common.collect.Maps;
import java.util.Map;

class IdentityHashMapBoxedKeyCheckGuavaSample {

  void boxedKeys() {
    Map<Boolean, String> booleans = Maps.newIdentityHashMap(); // Noncompliant {{Use a map that compares keys by value because IdentityHashMap compares keys by reference.}}
//                                  ^^^^^^^^^^^^^^^^^^^^^^^^^
    Map<Byte, String> bytes = Maps.newIdentityHashMap(); // Noncompliant
    Map<Character, String> characters = Maps.newIdentityHashMap(); // Noncompliant
    Map<Short, String> shorts = Maps.newIdentityHashMap(); // Noncompliant
    Map<Integer, String> integers = Maps.newIdentityHashMap(); // Noncompliant
    Map<Long, String> longs = Maps.newIdentityHashMap(); // Noncompliant
    Map<Float, String> floats = Maps.newIdentityHashMap(); // Noncompliant
    Map<Double, String> doubles = Maps.newIdentityHashMap(); // Noncompliant
  }

  void compliant() {
    Map<String, Boolean> boxedValue = Maps.newIdentityHashMap();
    Map<String, String> strings = Maps.newIdentityHashMap();
    Map<Object, String> objects = Maps.newIdentityHashMap();
  }

  <T> void genericKey() {
    Map<T, String> generic = Maps.newIdentityHashMap();
  }
}
