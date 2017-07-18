import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.Objects;

abstract class A {

  void foo(Map<String,Object> map, String key) {
    // Noncompliant@+1 [[flows=computeIfAbsent]] {{Replace this "Map.get()" and condition with a call to "Map.computeIfAbsent()".}}
    Object value = map.get(key);  // flow@computeIfAbsent [[order=1]] {{'Map.get()' is invoked.}}
    if (value == null) { // flow@computeIfAbsent [[order=2]] {{Implies 'value' can be null.}}
      map.put(key, new Object()); // flow@computeIfAbsent [[order=3]] {{'Map.put()' is invoked with same key.}}
    }
  }

  void bar(Map<String,Object> map, String key) {
    Object value = map.get(key); // Noncompliant {{Replace this "Map.get()" and condition with a call to "Map.computeIfPresent()".}}
    if (value != null) {
      map.put(key, new Object());
    }
  }

  void del(Map<String,Object> map, String key) {
    Object value = map.get(key); // Noncompliant - FP?
    Preconditions.checkState(value == null, "Value should always be null!");
    map.put(key, new Object());
  }

  void thr(Map<String,Object> map, String key) {
    Object value = map.get(key); // Noncompliant - FP?
    if (value == null) {
      throw new IllegalStateException("Value should always be null!");
    }
    map.put(key, new Object());
  }

  void qix(Map<String, Object> map, String key) {
    Object value = map.get(key); // Noncompliant
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

  void dbl(Map<String, Object> map, String key1, String key2) {
    Object value = map.get(key1); // Compliant - you can reach each put with NULL and NOT_NULL constraint on 'value'
    if (value == null) {
      map.put(key1, new Object());
    } else {
      map.put(key1, new Object());
    }
    doSomething(value);

    // Noncompliant@+1 [[flows=computeIfPresent]] {{Replace this "Map.get()" and condition with a call to "Map.computeIfPresent()".}}
    value = map.get(key2); // flow@computeIfPresent {{'Map.get()' is invoked.}}
    if (value == null) { // flow@computeIfPresent {{Implies 'value' is not null.}}
      map.put(key1, new Object());
    } else {
      map.put(key2, new Object()); // flow@computeIfPresent  {{'Map.put()' is invoked with same key.}}
    }
    doSomething(value);
  }

  abstract void doSomething(Object... objects);

}
