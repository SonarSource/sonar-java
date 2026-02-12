public class MultipleMainInstancesSample {
  public static class NonCompliant {
    public static void main(String[] args) { // Noncompliant
//  ^[el=+3;ec=5] 0
      System.out.println("Static main detected; shadowing instance main.");
    }

    void main() { // Noncompliant
//  ^[el=+3;ec=5] 0
      System.out.println("Unreachable entry point due to static precedence.");
    }
  }

  public static class Compliant {
    public static class LegacyApplication {
      // Compliant: Standard static entry point
      public static void main(String[] args) { // Compliant
        System.out.println("Running standard static entry point.");
      }
    }

    static class Application {
      // Compliant: Instance main method in a separate class
      void main() { // Compliant
        System.out.println("Running modern instance main entry point.");
      }
    }
  }

  public static enum NonCompliantEnum {
    INSTANCE;

    public static void main(String[] args) { // Noncompliant
      System.out.println("Static main in enum detected; shadowing instance main.");
    }

    void main() { // Noncompliant
      System.out.println("Unreachable entry point in enum due to static precedence.");
    }
  }

  public static interface NonCompliantInterface {
    static void main(String[] args) { // Noncompliant
      System.out.println("Static main in interface detected; shadowing instance main.");
    }

    default void main() { // Noncompliant
      System.out.println("Unreachable entry point in interface due to static precedence.");
    }
  }

  public static record NonCompliantRecord(String data) {
    public static void main(String[] args) { // Noncompliant
      System.out.println("Static main in record detected; shadowing instance main.");
    }

    void main() { // Noncompliant
      System.out.println("Unreachable entry point in record due to static precedence.");
    }
  }
}

// test implicit class
void main() { // Noncompliant
  System.out.println("Hello World!");
}

static public void main(String[] args) { // Noncompliant
  System.out.println("Hello World!");
}
