import java.util.Map;
import java.util.Map.Entry;

class A {
  private Integer a = 0;
  Object c = new A();

  private A getObject(A a) {
    return a;
  }

  void foo1(Object x) {
    if (x != null && x instanceof A) { // Noncompliant [[sc=9;ec=18]] {{Remove this unnecessary null check; "instanceof" returns false for nulls.}}
    }
    if (x instanceof A) { // Compliant
    }
    if (x == null || !(x instanceof A)) { // Noncompliant {{Remove this unnecessary null check; "instanceof" returns false for nulls.}}
    }
    if (x instanceof A || x == null) { // Compliant
    }
    if (!(x instanceof A) || x == null) { // Noncompliant [[sc=30;ec=39]] {{Remove this unnecessary null check; "instanceof" returns false for nulls.}}
    }
    if (getObject((A) c) instanceof A) { // Compliant
    }
    if (getObject((A) c) instanceof A && getObject((A) c) != null) { // Noncompliant
    }

    // coverage
    A alpha = new A();
    if (alpha.a != null && !(alpha instanceof A)) { // Compliant
    }
    if (alpha.a == null && (alpha instanceof A)) { // Compliant
    }
    if (alpha.a == 1 && (alpha instanceof A)) { // Compliant
    }
  }

  void foo2(A a) {
    if (a.c == null || a.c instanceof A) { // Compliant
    } else if (a.c != null) { // Compliant
    } else if (a != null && a.c instanceof A) { // Compliant
    } else if (null != a && a instanceof A) { // Noncompliant
    } else if (null == a || !(a instanceof A)) { // Noncompliant [[sc=16;ec=25]] {{Remove this unnecessary null check; "instanceof" returns false for nulls.}}
    } else if (a.c != null && a.c instanceof A) { // Noncompliant
    }
    while (a != null && a instanceof A) { // Noncompliant
      // ...
    }
  }

  void foo3(A a) {
    new Thread() {
      public void run() {
        if (a != null && a instanceof A) { // Noncompliant
          System.out.println("blah");
        }
      }
    }.start();
  }

  public boolean foo4(Object obj) {
    if (obj instanceof Entry) {
      Entry<?, ?> entry = (Entry<?, ?>) obj;
      return entry.getKey() != null // Compliant
        && entry.getValue() instanceof Map
        && entry.toString() != null;
    }
    return false;
  }
}
