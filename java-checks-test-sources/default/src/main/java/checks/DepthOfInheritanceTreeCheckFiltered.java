package checks;

class DepthOfInheritanceTreeCheckFiltered extends OneFiltered {

  void foo() {
    Object o = new DepthOfInheritanceTreeCheckFiltered() {}; // Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}
  }
}

class OneFiltered extends TwoFiltered {}
class TwoFiltered {}

enum AnyFiltered {
  enumConst { // Compliant - enum constant are not considered (level=3: enumConst -> Any -> Enum<Any> -> Object)

    class Dit2Filtered extends OneFiltered {}

    @Override
    public String getString() {
      return "string";
    }
  };

  public abstract String getString();
}
