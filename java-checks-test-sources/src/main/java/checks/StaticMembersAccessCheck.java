package checks;

class StaticMembersAccessCheckA {
  public static int counter = 0;
  public int nonStaticCounter = 0;
  public StaticMembersAccessCheckC c = new StaticMembersAccessCheckC();
  public StaticMembersAccessCheckD d = new StaticMembersAccessCheckD();
  public class StaticMembersAccessCheckC {
    public int counter = 0;
    public StaticMembersAccessCheckD d = new StaticMembersAccessCheckD();
  }
  public static class StaticMembersAccessCheckD {
    public static int counter = 0;
    public static class StaticMembersAccessCheckE {
      public static int counter = 0;
    }
  }
  public static int method() {
    return 0;
  }
  public StaticMembersAccessCheckD d() {
    return d;
  }
}

class StaticMembersAccessCheckB {
  private StaticMembersAccessCheckA first = new StaticMembersAccessCheckA();
  private StaticMembersAccessCheckA second = new StaticMembersAccessCheckA();
  private StaticMembersAccessCheckA.StaticMembersAccessCheckD third = new StaticMembersAccessCheckA.StaticMembersAccessCheckD();

  public StaticMembersAccessCheckA.StaticMembersAccessCheckD d() {
    return third;
  }

  public void noncompliant() {
    first.counter ++; // Noncompliant [[sc=5;ec=18]] {{Change this instance-reference to a static reference.}}
    second.counter ++; // Noncompliant
    second.method(); // Noncompliant
    third.counter ++; // Noncompliant
    first.d.counter++; // Noncompliant
    first.c.d.counter++; // Noncompliant
    first.d().counter++; // Noncompliant
    d().counter++; // Noncompliant
    ((StaticMembersAccessCheckA.StaticMembersAccessCheckD) d()).counter++; // Noncompliant [[sc=5;ec=72]]
    (d()).counter++; // Noncompliant
    StaticMembersAccessCheckA.StaticMembersAccessCheckD[] darray = new StaticMembersAccessCheckA.StaticMembersAccessCheckD[1];
    darray[0].counter++; // Noncompliant
  }

  public void compliant() {
    StaticMembersAccessCheckA.counter ++;
    StaticMembersAccessCheckA.StaticMembersAccessCheckD.counter ++;
    StaticMembersAccessCheckA.StaticMembersAccessCheckD.StaticMembersAccessCheckE.counter ++;
    first.nonStaticCounter ++;
    first.c.counter ++;
  }
}
abstract class StaticMembersAccessCheckTest {

  void test(java.util.function.Supplier<?> s) {
    this.call(Object::new);
    this.call(() -> new Object());
    this.call(s);
  }

  static void call(java.util.Set<?> o) {}

  abstract void call(java.util.function.Supplier<?> supplier);
}
