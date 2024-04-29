package checks;

import java.nio.file.Path;
import java.nio.file.Paths;

class StaticMembersAccessCheckSampleA {
  public static int counter = 0;
  public int nonStaticCounter = 0;
  public StaticMembersAccessCheckSampleC c = new StaticMembersAccessCheckSampleC();
  public StaticMembersAccessCheckSampleD d = new StaticMembersAccessCheckSampleD();
  public class StaticMembersAccessCheckSampleC {
    public int counter = 0;
    public StaticMembersAccessCheckSampleD d = new StaticMembersAccessCheckSampleD();
  }
  public static class StaticMembersAccessCheckSampleD {
    public static int counter = 0;
    public static class StaticMembersAccessCheckSampleE {
      public static int counter = 0;
    }
  }
  public static int method() {
    return 0;
  }
  public StaticMembersAccessCheckSampleD d() {
    return d;
  }
}

class StaticMembersAccessCheckSampleB {
  private StaticMembersAccessCheckSampleA first = new StaticMembersAccessCheckSampleA();
  private StaticMembersAccessCheckSampleA second = new StaticMembersAccessCheckSampleA();
  private StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD third = new StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD();

  public StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD d() {
    return third;
  }

  public void noncompliant() {
    first.counter ++; // Noncompliant [[quickfixes=qf1]] {{Change this instance-reference to a static reference.}}
//  ^^^^^
    // fix@qf1 {{Replace "first" by "StaticMembersAccessCheckSampleA"}}
    // edit@qf1 [[sc=5;ec=10]] {{StaticMembersAccessCheckSampleA}}
    second.counter ++; // Noncompliant
    second.method(); // Noncompliant [[quickfixes=qf2]]
//  ^^^^^^
    // fix@qf2 {{Replace "second" by "StaticMembersAccessCheckSampleA"}}
    // edit@qf2 [[sc=5;ec=11]] {{StaticMembersAccessCheckSampleA}}
    third.counter ++; // Noncompliant
    first.d.counter++; // Noncompliant
    first.c.d.counter++; // Noncompliant [[quickfixes=qf3]]
//  ^^^^^^^^^
    // fix@qf3 {{Replace the expression by "StaticMembersAccessCheckSampleD"}}
    // edit@qf3 [[sc=5;ec=14]] {{StaticMembersAccessCheckSampleD}}
    first.d().counter++; // Noncompliant [[quickfixes=qf4]]
//  ^^^^^^^^^
    // fix@qf4 {{Replace the expression by "StaticMembersAccessCheckSampleD"}}
    // edit@qf4 [[sc=5;ec=14]] {{StaticMembersAccessCheckSampleD}}
    d().counter++; // Noncompliant

    ( // Noncompliant@+1 [[quickfixes=qf5]]
//^[sc=5;ec=6;sl=56;el=59]
      (StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD) d()
    ).counter++;
    // fix@qf5 {{Replace the expression by "StaticMembersAccessCheckSampleD"}}
    // edit@qf5 [[sc=5;el=+2;ec=6]] {{StaticMembersAccessCheckSampleD}}
    (d()).counter++; // Noncompliant
    StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD[] darray = new StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD[1];
    darray[0].counter++; // Noncompliant [[quickfixes=qf6]]
//  ^^^^^^^^^
    // fix@qf6 {{Replace the expression by "StaticMembersAccessCheckSampleD"}}
    // edit@qf6 [[sc=5;ec=14]] {{StaticMembersAccessCheckSampleD}}
    Path path = Paths.get("abc");
    char separator = path.toFile().separatorChar; // Noncompliant [[quickfixes=qf7]]
//                   ^^^^^^^^^^^^^
    // fix@qf7 {{Replace the expression by "File"}}
    // edit@qf7 [[sc=22;ec=35]] {{File}}
    // edit@qf7 [[sl=3;sc=1;el=3;ec=1]] {{import java.io.File;\n}}
  }

  public void compliant() {
    StaticMembersAccessCheckSampleA.counter ++;
    StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD.counter ++;
    StaticMembersAccessCheckSampleA.StaticMembersAccessCheckSampleD.StaticMembersAccessCheckSampleE.counter ++;
    first.nonStaticCounter ++;
    first.c.counter ++;
  }
}
abstract class StaticMembersAccessCheckSampleTest {

  void test(java.util.function.Supplier<?> s) {
    this.call(Object::new);
    this.call(() -> new Object());
    this.call(s);
  }

  static void call(java.util.Set<?> o) {}

  abstract void call(java.util.function.Supplier<?> supplier);
}
