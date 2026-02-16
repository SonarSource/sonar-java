public class MultipleMainInstancesSample {
  public static class NonCompliant {
    public static void main(String[] args) { // Noncompliant {{At most one main method should be defined in a class.}}
//                     ^^^^
      System.out.println("Static main detected; shadowing instance main.");
    }

    void main() {
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

    void main() {

      System.out.println("Unreachable entry point in enum due to static precedence.");
    }
  }

  public static interface NonCompliantInterface {
    static void main(String[] args) { // Noncompliant
      System.out.println("Static main in interface detected; shadowing instance main.");
    }

    default void main() {

      System.out.println("Unreachable entry point in interface due to static precedence.");
    }
  }

  public static record NonCompliantRecord(String data) {
    public static void main(String[] args) { // Noncompliant
      System.out.println("Static main in record detected; shadowing instance main.");
    }

    void main() {
      System.out.println("Unreachable entry point in record due to static precedence.");
    }
  }

  public static class NonCompliantWithOverloads {
    class Parent {
      void main() {
        System.out.println("Parent instance main method.");
      }
    }

    class Child extends Parent {
      public static void main(String[] args) { // Noncompliant {{Main method should not be defined in a class if a main method is already defined in a superclass.}}
        System.out.println("Static main in child class detected; shadowing instance main.");
      }
    }
  }

  public static class CompliantWithOverloads {
    class Parent {
      void main() {
        System.out.println("Parent instance main method.");
      }
    }

    class Child extends Parent {
      @Override
      void main() { // Compliant: This is an instance method that overrides the parent main
        System.out.println("Child instance main method overriding parent main.");
      }
    }
  }

  public static class TwoMainsInParentOneOverriddenInChild {
    class Parent {
      void main() { // Noncompliant {{At most one main method should be defined in a class.}}
        System.out.println("Parent void main method.");
      }

      void main(String[] args) {
        System.out.println("Parent String args main method.");
      }
    }

    class Child extends Parent {
      @Override
      void main() { // Compliant: This is an instance method that overrides the parent main
        System.out.println("Child instance main method overriding parent main.");
      }
    }
  }

  public static class TwoMainsInParentTwoOverriddenInChild {
    class Parent {
      void main() { // Noncompliant {{At most one main method should be defined in a class.}}
        System.out.println("Parent void main method.");
      }

      void main(String[] args) {
        System.out.println("Parent String args main method.");
      }
    }

    class Child extends Parent {
      @Override
      void main() { // Noncompliant {{At most one main method should be defined in a class.}}
        System.out.println("Child instance main method overriding parent main.");
      }

      @Override
      void main(String[] args) {
        System.out.println("Child String args main method overriding parent main.");
      }
    }
  }

  public static class ChainedOverrides {
    class GrandParent {
      void main() {
        System.out.println("Parent void main method.");
      }
    }

    class Parent extends GrandParent {
    }

    class NonCompliantChild extends Parent {
      void main(String[] args) { // Noncompliant {{Main method should not be defined in a class if a main method is already defined in a superclass.}}
        System.out.println("Child main method detected; shadowing grandparent main.");
      }
    }

    class CompliantChild extends Parent {
      @Override
      void main() { // Compliant: This is an instance method that overrides the parent main
        System.out.println("Child instance main method overriding grandparent main.");
      }
    }
  }
}

// test implicit class
void main() { // Noncompliant
  System.out.println("Hello World!");
}

static public void main(String[] args) {
  System.out.println("Hello World!");
}
