class MethodYields {
  public boolean method(Object a, boolean b) {
    boolean result = true;
    if(a != null) {
      if (b == true) {
        result = false;
      } else {
        return b;
      }
    }
    return result;
  }
}
