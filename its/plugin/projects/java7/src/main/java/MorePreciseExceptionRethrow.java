class MorePreciseExceptionRethrow {

  static class Exception1 extends Exception {}
  static class Exception2 extends Exception {}

  public void example(boolean f) throws Exception1, Exception2 {
    try {
      if (f) {
        throw new Exception1();
      } else {
        throw new Exception2();
      }
    } catch (Exception e) {
      throw e; // Java 6 : unreported exception Exception; must be caught or declared to be thrown
    }
  }

}
