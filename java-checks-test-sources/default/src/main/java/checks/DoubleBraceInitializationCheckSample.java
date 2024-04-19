package checks;

import java.util.HashMap;
import java.util.Map;

class DoubleBraceInitializationCheckSample {
  Map<String, String> source = new HashMap() {{ // Noncompliant [[sc=46;el=+3;ec=5]] {{Use another way to initialize this instance.}}
    put("firstName", "John");
    put("lastName", "Smith");
  }};

  DoubleBraceInitializationCheckSampleB b0 = new DoubleBraceInitializationCheckSampleB();
  DoubleBraceInitializationCheckSampleB b1 = new DoubleBraceInitializationCheckSampleB() {{ bar("hello"); }}; // Noncompliant [[sc=90;ec=109]] {{Use another way to initialize this instance.}}
  DoubleBraceInitializationCheckSampleB b2 = new DoubleBraceInitializationCheckSampleB() {{
      field = -1;
    }
    @Override
    void foo() {};
  };
  DoubleBraceInitializationCheckSampleB b3 = new DoubleBraceInitializationCheckSampleB() {
    @Override
    void foo() {}
  };
}

class DoubleBraceInitializationCheckSampleB {
  int field;
  void foo() {}
  void bar(String s) {}
}
