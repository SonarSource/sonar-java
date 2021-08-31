package checks;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    first.counter ++; // Noncompliant [[sc=5;ec=18;quickfixes=qf1]] {{Change this instance-reference to a static reference.}}
    // fix@qf1 {{Replace "first" by "StaticMembersAccessCheckA"}}
    // edit@qf1 [[sc=5;ec=10]] {{StaticMembersAccessCheckA}}
    second.counter ++; // Noncompliant
    second.method(); // Noncompliant [[sc=5;ec=18;quickfixes=qf2]]
    // fix@qf2 {{Replace "second" by "StaticMembersAccessCheckA"}}
    // edit@qf2 [[sc=5;ec=11]] {{StaticMembersAccessCheckA}}
    third.counter ++; // Noncompliant
    first.d.counter++; // Noncompliant
    first.c.d.counter++; // Noncompliant [[sc=5;ec=22;quickfixes=qf3]]
    // fix@qf3 {{Replace "first.c.d" by "StaticMembersAccessCheckD"}}
    // edit@qf3 [[sc=5;ec=14]] {{StaticMembersAccessCheckD}}
    first.d().counter++; // Noncompliant // Noncompliant [[sc=5;ec=22;quickfixes=qf4]]
    // fix@qf4 {{Replace "first.d()" by "StaticMembersAccessCheckD"}}
    // edit@qf4 [[sc=5;ec=14]] {{StaticMembersAccessCheckD}}
    d().counter++; // Noncompliant
    // Noncompliant@+1 [[sc=5;el=+3;ec=14;quickfixes=qf5]]
    (
      (StaticMembersAccessCheckA.StaticMembersAccessCheckD) d()
    ).counter++;
    // fix@qf5 {{Replace "( (StaticMembersAccessCheckA.StaticMembersAccessCheckD) d() )" by "StaticMembersAccessCheckD"}}
    // edit@qf5 [[sc=5;el=+2;ec=6]] {{StaticMembersAccessCheckD}}
    (d()).counter++; // Noncompliant
    StaticMembersAccessCheckA.StaticMembersAccessCheckD[] darray = new StaticMembersAccessCheckA.StaticMembersAccessCheckD[1];
    darray[0].counter++; // Noncompliant [[sc=5;ec=22;quickfixes=qf6]]
    // fix@qf6 {{Replace "darray[0]" by "StaticMembersAccessCheckD"}}
    // edit@qf6 [[sc=5;ec=14]] {{StaticMembersAccessCheckD}}
    Path path = Paths.get("abc");
    char separator = path.toFile().separatorChar; // Noncompliant [[sc=22;ec=49;quickfixes=qf7]]
    // fix@qf7 {{Replace "path.toFile()" by "File"}}
    // edit@qf7 [[sc=22;ec=35]] {{File}}
    // edit@qf7 [[sl=3;sc=1;el=3;ec=1]] {{import java.io.File;\n}}
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
