package checks;

import java.io.IO;

public class InitializeSubclassFieldsBeforeSuperSample {
  abstract static class Parent {
    protected Parent() {
      IO.println("Hello from parent, name is " + getName());
    }

    abstract String getName();
  }

  public static class NonCompliant extends Parent {
    private final String name;

    NonCompliant(String name) {
      super();
      this.name = name; // Noncompliant
    }


    @Override
    String getName() {
      return name;
    }
  }

  public static class Compliant extends Parent {
    private final String name;

    Compliant(String name) {
      this.name = name;
      super();
    }

    @Override
    String getName() {
      return name;
    }
  }
}
