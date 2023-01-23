import com.google.common.annotations.VisibleForTesting;

class Foo {
  private int foo1;
  int foo2;
  protected int foo3;
  public int foo4; // Noncompliant [[sc=14;ec=18]] {{Make foo4 a static final constant or non-public and provide accessors if needed.}}

  public static int foo5; // Noncompliant {{Make foo5 a static final constant or non-public and provide accessors if needed.}}
  public final int foo6; // Compliant

  private static final int bar1;
  static final int bar2;
  protected static final int bar3;
  public static final int bar4; // Compliant
  public final static int bar5; // Compliant

  @VisibleForTesting
  public int foo; // Compliant
  static interface blah {
    public String howdy = "Well, hello there!";
  }

  public long l1; // Noncompliant {{Make l1 a static final constant or non-public and provide accessors if needed.}}
}

interface bar {
  public int blah = 0;

}

class Bar {
  public int foo; // Noncompliant [[sc=14;ec=17;quickfixes=qf1]]
  // fix@qf1 {{Replace public modifier with private}}
  // edit@qf1 [[sc=3;ec=9]] {{private}}
  
  static public int foo; // Noncompliant [[sc=21;ec=24;quickfixes=qf2]]
  // fix@qf2 {{Replace public modifier with private}}
  // edit@qf2 [[sc=10;ec=16]] {{private}}
  
}
