package test;

import java.util.List;
import java.util.ArrayList;

public class DoubleCheckedLockingAssignment {

  private volatile List<String> strings;

  public List<String> getStrings() {
    if (strings == null) {
      synchronized (this) {
        if (strings == null) {
          strings = new ArrayList<>();  // Noncompliant [[sc=11;ec=38;secondary=15,16]] {{Fully initialize "strings" before assigning it.}}
          strings.add("Hello");
          strings.add("World");
        }
      }
    }
    return strings;
  }

  public List<String> getStringsBis() {
    if (null == strings) {
      synchronized (this) {
        if (null == this.strings) {
          strings = new ArrayList<>();  // Noncompliant [[sc=11;ec=38;secondary=28,30]] {{Fully initialize "strings" before assigning it.}}
          strings.add("Hello");
          System.out.println();
          strings.add("World");
        }
      }
    }
    return strings;
  }

  public List<String> lambda() {
    if (strings == null) {
      synchronized (this) {
        if (strings == null) {
          strings = new ArrayList<>();  // Compliant
          Runnable run = () -> System.out.println(strings);
          run = new Runnable() {
            public void run() {
              System.out.println(strings);
            }
          };
        }
      }
    }
    return strings;
  }

  public List<String> getStrings2() {
    if (strings == null) {
      synchronized (this) {
        if (strings == null) {
          List<String> tmpList = new ArrayList<>(); // Compliant
          tmpList.add("Hello");
          tmpList.add("World");
          strings = tmpList;
        }
      }
    }
    return strings;
  }

  private volatile List<String> strings2;

  public List<String> coverage(List<String> param) {
    if (strings == null) {
      strings = new ArrayList<>();  // Compliant
      strings.add("Hello");
      strings.add("World");
    }
    if (strings == null) {

    }
    if (strings == null) {
      System.out.printf("");
    }
    if (strings == null) {
      synchronized (this) {

      }
    }
    if (param == null) {
      synchronized (this) {
        if (param == null) {

        }
      }
    }
    if (param != null) {
      synchronized (this) {
        if (param == null) {

        }
      }
    }
    if (strings == null) {
      synchronized (this) {
        System.out.println();
      }
    }
    if (strings == null) {
      synchronized (this) {
        if (strings2 == null) {

        }
      }
    }
    if (strings == null) {
      synchronized (this) {
        if (strings == null) System.out.println();
      }
    }
    if (strings == null) {
      synchronized (this) {
        if (strings == null) {
          strings2 = new ArrayList<>();
          strings.add("Hello");
          strings.add("World");
        }
      }
    }
    if (strings == null) {
      synchronized (this) {
        if (strings == null) while (true) {};
      }
    }
    if (strings == null) {
      synchronized (this) {
        if (strings == strings2) while (true) {};
      }
    }
    if (null == null) {
      synchronized (this) {
        if (strings == strings2) while (true) {};
      }
    }
  }
}
