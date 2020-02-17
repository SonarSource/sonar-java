package checks;

import java.util.HashMap;
import java.util.Map;

class DoubleBraceInitializationCheck {
  Map<String, String> source = new HashMap() {{ // Noncompliant [[sc=46;el=+3;ec=5]] {{Use another way to initialize this instance.}}
    put("firstName", "John");
    put("lastName", "Smith");
  }};

  DoubleBraceInitializationCheckB b0 = new DoubleBraceInitializationCheckB();
  DoubleBraceInitializationCheckB b1 = new DoubleBraceInitializationCheckB() {{ bar("hello"); }}; // Noncompliant [[sc=78;ec=97]] {{Use another way to initialize this instance.}}
  DoubleBraceInitializationCheckB b2 = new DoubleBraceInitializationCheckB() {{
      field = -1;
    }
    @Override
    void foo() {};
  };
  DoubleBraceInitializationCheckB b3 = new DoubleBraceInitializationCheckB() {
    @Override
    void foo() {}
  };
}

class DoubleBraceInitializationCheckB {
  int field;
  void foo() {}
  void bar(String s) {}
}
