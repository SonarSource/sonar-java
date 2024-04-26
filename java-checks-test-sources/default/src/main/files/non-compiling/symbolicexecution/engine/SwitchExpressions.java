package symbolicexecution.engine;

import org.foo.SwitchExpressions.MyEnum;

class SwitchExpressions {

  void without_default(MyEnum e) {
    Object res = new Object();
    res = switch(e) { // flow@se_1 {{Implies 'res' is null.}}
      case A -> null;
      case B -> new Object();
    };
 // Noncompliant@+1
    res.toString(); // flow@se_1 {{'res' is dereferenced.}}
  }

  void control_without_default(MyEnum e) {
    Object res = null;
    res = switch(e) {
      case A -> new Object();
      case B -> new Object();
    };
    res.toString(); // Compliant
  }

  void with_default(MyEnum e) {
    Object res = new Object();
    res = switch(e) { // flow@se_2 {{Implies 'res' is null.}}
      case A -> null;
      default -> new Object();
    };
 // Noncompliant@+1
    res.toString(); // flow@se_2 {{'res' is dereferenced.}}
  }

  void control_with_default(MyEnum e) {
    Object res = null;
    res = switch(e) {
      case A -> new Object();
      default -> new Object();
    };
    res.toString(); // Compliant
  }

  enum MyEnum {
    A, B
  }
}
