package checks;

class KeySetInsteadOfEntrySetCheck extends java.util.HashMap<String, Object> {
  KeySetInsteadOfEntrySetCheck inner;
  java.util.HashMap<String, Object> map;
}

class KeySetInsteadOfEntrySetCheckExtendedClass extends KeySetInsteadOfEntrySetCheck {

  public class InnerClass {
    InnerClass inner;
    java.util.HashMap<String, Object> map;
  }

  private String key;

  private String getKey() {
    return key;
  }

  private java.util.List<String> list;

  private java.util.List<String> list() {
    return list;
  }

  java.util.HashMap<String, Object> map, map2;
  java.util.HashMap<String, Object>[] map3;

  InnerClass inner;

  public void method() {
    for (String value : list) { // Compliant
    }
    for (String value : list()) { // Compliant
    }
    for (String value : new String[] {}) { // Compliant
    }
    for (String key2 : map.keySet()) { // Compliant
      Object value1 = map.get(key);
      Object value2 = map.get(getKey());
      Object value3 = map2.get(key2);
    }
    for (String key5 : this.map3[0].keySet()) { // Compliant, false negative
      Object value = this.map3[0].get(key5);
    }
    for (String key5 : inner.inner.map.keySet()) { // Compliant, false negative
      Object value = inner.inner.map.get(key5);
    }
    for (String key5 : keySet()) { // Compliant, false negative
      Object value = map3[0].get(key5);
    }
    for (String key5 : super.inner.map.keySet()) { // Compliant, false negative
      Object value = super.inner.map.get(key5);
    }
    for (String key5 : this.inner.map.keySet()) { // Compliant, false negative
      Object value = this.inner.map.get(key5);
    }
    for (java.util.Map.Entry<String, Object> key6 : map.entrySet()) { // Compliant
      Object value = map.get(key6);
    }

    for (String key3 : keySet()) { // Noncompliant {{Iterate over the "entrySet" instead of the "keySet".}}
      Object value = get(key3);
    }
    for (String key4 : this.keySet()) { // Noncompliant {{Iterate over the "entrySet" instead of the "keySet".}}
      Object value = this.get(key4);
    }
    for (String key5 : super.keySet()) { // Noncompliant {{Iterate over the "entrySet" instead of the "keySet".}}
//  ^^^
      Object value = super.get(key5);
    }
    for (String key5 : map.keySet()) { // Noncompliant {{Iterate over the "entrySet" instead of the "keySet".}}
      Object value = map.get(key5);
    }
    for (String key5 : super.map.keySet()) { // Noncompliant {{Iterate over the "entrySet" instead of the "keySet".}}
      Object value = super.map.get(key5);
    }
    for (String key5 : super.map.keySet()) { // Compliant
      Object value = this.map.get(key5);
    }
    for (String key5 : this.map.keySet()) { // Noncompliant {{Iterate over the "entrySet" instead of the "keySet".}}
      Object value = this.map.get(key5);
    }
  }

}
