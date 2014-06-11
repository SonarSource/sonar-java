class A {
  Object a;
  private void method(){
    try {
      a.equals(null);
    } catch(NullPointerException npe) {
      log.info("argument was null");
    }
    try {
      a.equals(null);
    } catch(java.lang.NullPointerException npe) {
      log.info("argument was null");
    }
    try {
      a.equals(null);
    } catch(java.lang.NullPointerException | RuntimeException runtimeException) {
      log.info("argument was null");
    }
  }
}