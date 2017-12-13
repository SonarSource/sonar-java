import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;

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

  abstract void doSomething(Object... objects);
  @CheckForNull
  abstract Object getValue();

}

abstract class MyMap<K, V> implements Map<K, V> {

  public V get() {
    return null;
  }

  public void put(Object o) {
    // do nothing
  }
}

abstract class ExceptionThrown {
  void foo(java.util.Map<String, Object> items, String key) throws MyException {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, bar()); // Compliant, bar() can throw a checked exception, so it can not be extracted to a lambda
    }
  }
  abstract String bar() throws MyException;
  abstract String bar2() throws MyRuntimeException;
  abstract String bar4() throws UnknownException;
  static class MyException extends Exception { }
  static class MyRuntimeException extends RuntimeException { }
  void foo2(java.util.Map<String, Object> items, String key) throws MyException {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, unknown_method()); // Compliant, unknown method so put is not resolved
    }
  }
  void foo3(java.util.Map<String, Object> items, String key) {
    Object value = items.get(key); // Noncompliant (bar2 is throwing a runtime exception)
    if (value == null) {
      items.put(key, bar2());
    }
  }
  void foo4(java.util.Map<String, Object> items, String key) {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, bar4()); // compliant : exception thrown is unknown
    }
  }

  void foo5(java.util.Map<String, UnknownObject> items, String key) throws MyException {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, unknown_method()); // Compliant, unknown method so put is not resolved
    }
  }
}
