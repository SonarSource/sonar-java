package checks;

public class DepthOfInheritanceTreeCheck extends One { // Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}

  void foo() {
    Object o = new DepthOfInheritanceTreeCheck() {}; // Noncompliant {{This class has 4 parents which is greater than 2 authorized.}}
  }
}

class One extends Two {}
class Two {}

enum Any {
  enumConst { // Compliant - enum constant are not considered (level=3: enumConst -> Any -> Enum<Any> -> Object)

    class Dit2 extends One {} // Noncompliant

    @Override
    public String getString() {
      return "string";
    }
  };

  public abstract String getString();
}
