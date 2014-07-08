import com.google.common.annotations.VisibleForTesting;

class Foo {
  private int foo1;
  int foo2;
  protected int foo3;
  public int foo4; // Non-Compliant

  public static int foo5; // Non-Compliant
  public final int foo6; // Non-Compliant

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

  public long l1;
}

interface bar {
  public int blah = 0;

}