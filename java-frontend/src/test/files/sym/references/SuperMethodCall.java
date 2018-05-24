public interface A {
  default String f() {
    return "A";
  }
}

public interface B {
  default String f() {
    return "B";
  }
}

public class C {
  public String f() {
    return "C";
  }
}

public class D extends C implements A, B {
  public void call() {
    f();
  }
  @Override
  public String f() {
    return super.f() +
           A.super.f() +
           B.super.f();
  }

}
