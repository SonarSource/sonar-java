package checks;

public class MultipleMainInstancesSample {
  public static class NonCompliant {
    public static void main(String[] args) { // Noncompliant
      System.out.println("Static main detected; shadowing instance main.");
    }

    void main() { // Noncompliant
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
}
