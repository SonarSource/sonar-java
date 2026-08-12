import java.util.function.IntUnaryOperator;

class BadLocalVariableName {

  // Instance/class fields are not checked by this rule at all, regardless of underscores
  int _instanceField;
  static int _staticField;

  BadLocalVariableName(
    int godParam,
    int BAD_CONSTRUCTOR_PARAM, // Noncompliant

    int _goodConstructorParam // Noncompliant

  ) {}

  void method(
    int goodParam,
    int BAD_FORMAL_PARAMETER, // Noncompliant

    int _goodParam // Noncompliant

  ) {
    int BAD; // Noncompliant
    int good;
    int _good; // Compliant, leading underscore stripped before check
    int good_; // Compliant, trailing underscore stripped before check
    int __myVar__; // Compliant, surrounding underscores stripped before check
    int _BAD; // Noncompliant

    int BAD_; // Noncompliant


    for (int I = 0; I < 10; I++) {
      int D; // Noncompliant
    }

    for (good = 0; good < 10; good++) {
    }

    try (Closeable BAD_RESOURCE = open()) { // Noncompliant
    } catch (IOException BAD_EXCEPTION) { // Noncompliant
    } catch (IllegalStateException E) { // compliant
    } catch (Exception EX) { // Noncompliant
    }
  }

  Object FIELD_SHOULD_NOT_BE_CHECKED = new Object(){
    {
      int BAD; // Noncompliant
    }
  };

  void forEachMethod() {
    int MY_CONSTANT_IS_NOT_A_CONSTANT = 21; // Noncompliant
    final int MY_LOCAL_CONSTANT = 42; // Noncompliant
    final String MY_OTHER_LOCAL_CONSTANT = "42"; // Noncompliant
    final Integer MY_ALTERNATE_CONSTANT = Integer.valueOf(42); // Noncompliant
    for (byte C : "".getBytes()) {
      int D; // Noncompliant
    }
  }

  void foo() {
    IntUnaryOperator f1 = (int _) -> 0; // Compliant, unnamed variable
    IntUnaryOperator f2 = _ -> 0; // Compliant, unnamed variable
    IntUnaryOperator f3 = _good -> 0; // Compliant, leading underscore stripped before check
    IntUnaryOperator f4 = good_ -> 0; // Compliant, trailing underscore stripped before check
    IntUnaryOperator f5 = _BAD -> 0; // Noncompliant

  }

  // Method names are not checked by this rule
  void _underscoreMethod() {
    int good;
  }
}
