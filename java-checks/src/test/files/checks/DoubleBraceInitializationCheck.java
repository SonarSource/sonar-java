import java.util.HashMap;
import java.util.Map;

class A {
  Map<String, String> source = new HashMap() {{ // Noncompliant [[sc=46;el=+3;ec=5]] {{Use another way to initialize this instance.}}
    put("firstName", "John");
    put("lastName", "Smith");
  }};

  B b0 = new B();
  B b1 = new B() {{ bar("hello"); }}; // Noncompliant [[sc=18;ec=37]] {{Use another way to initialize this instance.}}
  B b2 = new B() {{
      field = -1;
    }
    @Override
    void foo() {};
  };
  B b3 = new B() {
    @Override
    void foo() {}
  };
}

class B {
  int field;
  void foo() {}
  void bar(String s) {}
}
