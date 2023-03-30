package checks;

abstract class BrainMethodCheckWithLowerThresholds {

  interface AnInterface {
    void shouldBeIgnored();
  }

  void empty() {
  }

  public static native void alert(String msg) /* not JSNI comment */ /*-{
  for (i=0;i<=5;i++) {
  $wnd.alert(msg);
}
}-*/;

  void foo() { // Noncompliant [[sl=0;sc=3;el=+21;ec=4]] {{Refactor this brain method to reduce its complexity.}}
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 1: {
            System.out.println("quit");
          }
          default:
            return;
        }
      }
    } else {
      for (String s : new String[] {"a"}) {
        System.out.println(s);
      }
    }
    System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");
  }

  void boo() { // Compliant, not breaking LOC threshold
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        if (i > 2) {
          System.out.println("quit");
        }
      }
    }
  }

  void goo() { // Compliant, not breaking nesting threshold
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        System.out.println("1");
        System.out.println("2");
        System.out.println("3");
        System.out.println("4");
        System.out.println("5");
        System.out.println("6");
        System.out.println("7");
      }
    } else {
      for (String s : new String[] {"a"}) {
        System.out.println(s);
      }
    }
    System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");

  }

  void zoo() { // Compliant, not breaking number of variables threshold
    System.out.println("no more variable a");
    System.out.println("no more variable b");
    int x = 0;
    int y = 1;
    if (x < y) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 1: {
            System.out.println("quit");
          }
          default:
            return;
        }
      }
    } else {
      if (y == 2) {
        System.out.println("no more variable s and i");
      }
    }
    System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");
  }

  void hoo() { // Compliant, not breaking cyclomatic complexity
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (x > y) {
      if (y < 0) {
        try {
          System.out.println("ok");
        } catch (Exception e) {
          // TODO: handle exception
        }
      }
    }
    System.out.println("we");
    System.out.println("are");
    System.out.println("going");
    System.out.println("to");
    System.out.println("break");
    System.out.println("loc");
    System.out.println("threshold");
  }

  abstract void testAbs();

  @Override
  public boolean equals(Object obj) {
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 1: {
            System.out.println("quit");
          }
          default:
            return true;
        }
      }
    } else {
      for (String s : new String[] {"a"}) {
        System.out.println(s);
      }
    }
    System.out.println("Equals/hashCode methods are ignored");
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
