package checks;

public class UnnamedVariableShouldUseVarCheckSample {
  void enhancedForLoopGood(Iterable<String> ss) {
    for (var _ : ss) {
      action("pass");
    }
  }

  void enhancedForLoopBad(Iterable<String> ss) {
    for (String _ : ss) { // Noncompliant {{Use `var` instead of a type with unnamed variable _}} [[quickfixes=qf1]]
//       ^^^^^^
//       fix@qf1 {{Replace the type with "var"}}
//       edit@qf1 [[sc=10;ec=16]] {{var}}
      action("pass");
    }
  }

  void enhancedForLoopNotApplicable(Iterable<String> ss) {
    for (String s : ss) {
      action(s);
    }
  }

  void basicForLoop() {
    for (int i = 0; i < 5; i++) {
      action("pass");
    }
  }

  void basicForLoopWithUnnamedVariable() {
    for (int i = 0, _ = 10; i < 5; i++) {
      action("pass");
    }
  }

  void tryWithResourcesGood() {
    try(var _ = new MyResource("one")) {
      action("pass");
    } catch (Exception e) {}
  }

  void tryWithResourcesBad() {
    try(MyResource _ = new MyResource("one")) { // Noncompliant {{Use `var` instead of a type with unnamed variable _}} [[quickfixes=qf2]]
//      ^^^^^^^^^^
//      fix@qf2 {{Replace the type with "var"}}
//      edit@qf2 [[sc=9;ec=19]] {{var}}
      action("pass");
    } catch (Exception e) {}
  }

  void tryWithResourcesMultipleBad() {
    try(MyResource _ = new MyResource("one"); // Noncompliant {{Use `var` instead of a type with unnamed variable _}} [[quickfixes=qf3]]
//      ^^^^^^^^^^
//      fix@qf3 {{Replace the type with "var"}}
//      edit@qf3 [[sc=9;ec=19]] {{var}}
        MyResource _ = new MyResource("two")) { // Noncompliant {{Use `var` instead of a type with unnamed variable _}} [[quickfixes=qf4]]
//      ^^^^^^^^^^
//      fix@qf4 {{Replace the type with "var"}}
//      edit@qf4 [[sc=9;ec=19]] {{var}}
      action("pass");
    } catch (Exception e) {}
  }

  int tryNotApplicable(int a) {
    try {
      if (a == 10) {
        throw new IllegalArgumentException("no ten please");
      }
      return 2 * a;
    }
    catch (IllegalArgumentException iae) {
      return -1;
    }
  }

  void tryWithResourcesJava9Enhancement() {
    MyResource myResource = new MyResource("Java 9 style");
    try(myResource) {
      action("pass");
    } catch (Exception e) {}
  }

  void action(String s) {}

  static class MyResource implements AutoCloseable {
    MyResource(String name) {}

    @Override
    public void close() throws Exception {}
  }
}
