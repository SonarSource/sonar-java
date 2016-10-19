abstract class MethodBehavior {
  public boolean topMethod(boolean a) {
    boolean result = false;
    if(a) {
      result = bar(a);
    } else {
      result = foo(a);
    }
    qix();
    int i = "".length();
    return result;
  }

  private boolean foo(boolean a) {
    return a;
  }

  private boolean bar(boolean a) {
    return !a;
  }

  private abstract void qix();

  private void independent(){
  }

  private native int nativeMethod();

}
