abstract class MethodBehavior {
  static boolean topMethod(boolean a) {
    boolean result = false;
    if(a) {
      result = bar(a);
    } else {
      result = foo(a);
    }
    abstractMethod();
    int i = "".length();
    return result;
  }

  private boolean foo(boolean a) {
    return a;
  }

  private boolean bar(boolean a) {
    return !a;
  }

  abstract void abstractMethod();

  private void independent(){
  }

  private native int nativeMethod();

  public void publicMethod(boolean a) {
    boolean result = false;
    if(a) {
      result = bar(a);
    } else {
      result = foo(a);
    }
  }

}
