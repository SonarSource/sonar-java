package symbolicexecution.checks;

import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;

import java.util.Map;
import java.util.Objects;

abstract class MapComputeIfAbsentOrPresentCheckSample {

  void foo(Map<String,Object> map, String key) {
    // Noncompliant@+1 [[flows=computeIfAbsent]] {{Replace this "Map.get()" and condition with a call to "Map.computeIfAbsent()".}}
    Object value = map.get(key);  // flow@computeIfAbsent [[order=1]] {{'Map.get()' is invoked.}}
    if (value == null) { // flow@computeIfAbsent [[order=2]] {{Implies 'value' can be null.}}
      map.put(key, new Object()); // flow@computeIfAbsent [[order=3]] {{'Map.put()' is invoked with same key.}}
    }
  }

  void bar(Map<String,Object> map, String key) {
    Object value = map.get(key); // Noncompliant {{Replace this "Map.get()" and condition with a call to "Map.computeIfPresent()".}}
    if (null != value) {
      map.put(key, new Object());
    }
  }

  void del(Map<String,Object> map, String key) {
    Object value = map.get(key); // Compliant - throws an exception in case of null ness
    Preconditions.checkState(value == null, "Value should always be null!");
    map.put(key, new Object());
  }

  void thr(Map<String,Object> map, String key) {
    Object value = map.get(key); // Compliant - throws an exception in case of null ness
    if (value == null) {
      throw new IllegalStateException("Value should always be null!");
    }
    map.put(key, new Object());
  }

  void asd(Map<String,Object> map, String key) {
    Object value = map.get(key); // Compliant
    if (value == null) {
      value = map.get(key);
      if (value != null) {
        return;
      }
    }
    if (value == null) {
      map.put(key, new Object());
    }
  }

  void qix(Map<String, Object> map, String key) {
    Object value = map.get(key); // FN - requires explicit null-check
    if (Objects.isNull(value)) {
      map.put(key, new Object());
      doSomething(value); // required to keep 'value' alive and be able to retrieve constraint...
    }
  }

  void tmp(Map<String, Object> map1, Map<String, Object> map2, String key) {
    Object value = map1.get(key); // Compliant - different maps
    if (value == null) {
      map2.put(key, new Object());
    }
  }

  void gul(Map<String, Object> map, String key1, String key2) {
    Object value = map.get(key1); // Compliant - different keys
    if (value == null) {
      map.put(key2, new Object());
    }
  }

  void gro(Map<String, Object> map, String key) {
    Object value = map.get(key); // Noncompliant - reassigning everything
    Object value2 = value;
    if (value2 == null) {
      String key2 = key;
      Map<String, Object> map2 = map;
      map2.put(key2, new Object());
    }
  }

  void til(Map<String, Object> map, String key) {
    Object value = map.get(key); // Compliant - no constraint on value
    map.put(key, new Object());
  }

  void lol(Map<String, Object> map, String key1, String key2) {
    Object value = map.get(key1); // Noncompliant
    if (value == null) {
      map.get(key2);
      map.put(key1, new Object());
    }
  }

  void sal(Map<String, Object> map, String key) {
    Object value = map.get(key); // Compliant - you can reach put with NULL and NOT_NULL constraint on 'value'
    if (value == null) {
      doSomething();
    }
    map.put(key, new Object());
    doSomething(value);
  }

  void db1(Map<String, Object> map, String key1, String key2) {
    Object value = map.get(key1); // Compliant - you can reach each put with NULL and NOT_NULL constraint on 'value'
    if (value == null) {
      map.put(key1, new Object());
    } else {
      map.put(key1, new Object());
    }
    doSomething(value);

    value = map.get(key2);
    if (value == null) { // FN - requires if statement without else
      map.put(key1, new Object());
    } else {
      map.put(key2, new Object());
    }
    doSomething(value);
  }

  void db2(Map<String, Object> map, String key) {
    Object value = map.get(key); // Compliant
    if (value == null) {
      map.put(key, new Object());
    } else {
      doSomething(value); // value is used differntly if not null, but only added in the map if not present
    }
  }

  void nmp(MyMap<String, Object> map, String key) {
    Object value;

    value = map.get(); // Compliant - not the targeted 'put' and 'get' methods
    if (value == null) {
      map.put(new Object());
    }

    value = map.get(key); // Compliant - not the targeted 'put' method
    if (value == null) {
      map.put(new Object());
    }

    value = map.get(); // Compliant - not the targeted 'get' method
    if (value == null) {
      map.put(key, new Object());
    }
  }

  void nnp(Map<String, Object> map, String key) {
    if (map.containsKey(key)) { // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfPresent()".}}
      map.put(key, new Object());
    }
  }
  
  void npo(Map<String, Object> map, String key) { 
    if (!map.containsKey(key)) { // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
      map.put(key, new Object());
    }
  }

  void nnx(Map<String, Object> map, String key) {
    if (map.containsKey(key)) { // Has else branch
      map.put(key, new Object());
    } else {
    }
  }

  void nny(Map<String, Object> map, String key) {
    map.containsKey(key); // Compliant, no if statement 
    map.put(key, new Object());
  }
  
  void nnz(Map<String, Object> map, String key, boolean b) {
    map.containsKey(key); // Compliant, no if statement 
    if (b) {
      map.put(key, new Object());
    }
  }
  
  void nnq(Map<String, Object> map, String key, boolean b) {
    Object o = map.get(key);// Compliant, no if statement 
    if (b) { 
      map.put(key, new Object());
    }
  }

  void nnqx(Map<String, Object> map, String key1, String key2) {
    if (map.containsKey(key1)) {
      map.put(key2, new Object());
    }
  }

  void nwx(Map<String, Object> map, String key1) {
    Preconditions.checkState(map.containsKey(key1), "Value should always be null!");  
    map.put(key1, new Object());
  }

  void nwq(Map<String, Object> map, String key1) {
    Preconditions.checkState(!map.containsKey(key1), "Value should always be null!");  
    map.put(key1, new Object());
  }
  
  void npq(Map<String, Object> map, String key1) {
    map.containsKey(key1);
    if (!map.isEmpty()) {
      map.put(key1, new Object());
    }
  }

  void npqb(Map<String, Object> map, String key1) {
    map.containsKey(key1);
    if (is(key1)) {
      map.put(key1, new Object());
    }
  }
  
  void npqa(Map<String, Object> map, String key1) {
    boolean containsKey = map.containsKey(key1); // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfPresent()".}}
    if (containsKey) {
      map.put(key1, new Object());
    }
  }
  
  void npqc(Map<String, Object> map, String key1) {
    boolean containsKey = !map.containsKey(key1); // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
    if (containsKey) {
      map.put(key1, new Object());
    }
  }
  
  void npqd(Map<String, Object> map, String key1) {
    boolean containsKey = map.containsKey(key1); // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
    if (!containsKey) {
      map.put(key1, new Object());
    }
  }

  void npqz(Map<String, Object> map, String key1) {
    boolean containsKey = map.containsKey(key1); // Compliant, null won't be put when computeIfAbsent
    if (!containsKey) {
      map.put(key1, null);
    }

  }
  void npqy(Map<String, Object> map, String key1) {
    Object value = map.get(key1); // Compliant, null won't be put when computeIfAbsent
    if (value == null) {
      map.put(key1, null);
    }
  }

  void npqe(Map<String, Object> map, String key1) {
    boolean containsKey = map.containsKey(key1); // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
    boolean containsKey1 = map.containsKey(key1);
    if (!containsKey) {
      map.put(key1, new Object());
      map.put(key1, new Object());
    }
    if (!containsKey1) {
      map.put(key1, new Object());
      map.put(key1, new Object());
    }
  }
  
  void npqf(Map<String, Object> map, String key1) {
    boolean containsKey = map.containsKey(key1);
    boolean containsKey1 = map.containsKey(key1);
    if (!containsKey && containsKey1) {
      map.put(key1, new Object());
      map.put(key1, new Object());
    }
  }

  void npqg(Map<String, Object> map, String key1) {
    boolean containsKey = map.containsKey(key1);
    if (!containsKey) {
      map.put(key1, new Object());
    }
    if (containsKey) {
      map.put(key1, new Object());
    }
  }

  void multipleChecks(Map<String, Object> map, String key1, String key2, String key3, boolean b1, boolean b2) {
    if (b1) {
      if (!map.containsKey(key1))  // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
        map.put(key1, new Object());
      if (b2) {
        if (!map.containsKey(key2))  // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
          map.put(key2, new Object());

        if (!map.containsKey(key3)) // Noncompliant {{Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()".}}
          map.put(key3, new Object());
      }
    }
  }

  void checkEnum(Map<Enum, Object> map) {
    if (!map.containsKey(Enum.A)) // Noncompliant
      map.put(Enum.A, new Object());

    if (!map.containsKey(Enum.B)) // Noncompliant
      map.put(Enum.B, new Object());
  }
  
  enum Enum {
    A,
    B
  }
  
  abstract void doSomething(Object... objects);
  @CheckForNull
  abstract Object getValue();
  
  private boolean is(String key) {
    return true;
  }

}

abstract class MyMap<K, V> implements Map<K, V> {

  public V get() {
    return null;
  }

  public void put(Object o) {
    // do nothing
  }
}

