class EnclosingBlock {

  {
    // block
    myField = null;
    somethingElse();
  }

  static {
    // block
    myField = null;
    somethingElse();
  }

  String myField;

  java.util.function.Supplier<Object> s = () -> {
    // null
    myField = null;
  };

  public void fun() {
    // block
    myField = null;
    somethingElse();
  }

  public void fun2() {
    {
      // block
      String myField = null;
    }
    somethingElse();
  }

  private void somethingElse() {

  }
}

interface EnclosingMethodBlock {
  static final String myField;

  static final java.util.function.Supplier<Object> s = () -> {
    // null
    I1.myField = null;
  };

  default void fun() {
    // block
    I1.myField = null;
    somethingElse();
  }
}

