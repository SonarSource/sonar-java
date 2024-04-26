package checks;

import java.util.HashMap;
import java.util.Map;

class DoubleBraceInitializationCheckSample {
  Map<String, String> source = new HashMap() {{ // Noncompliant {{Use another way to initialize this instance.}}
^[sc=46;ec=5;sl=7;el=10]
    put("firstName", "John");
    put("lastName", "Smith");
  }};

  DoubleBraceInitializationCheckSampleB b0 = new DoubleBraceInitializationCheckSampleB();
  DoubleBraceInitializationCheckSampleB b1 = new DoubleBraceInitializationCheckSampleB() {{ bar("hello"); }}; // Noncompliant {{Use another way to initialize this instance.}}
//                                                                                       ^^^^^^^^^^^^^^^^^^^
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
