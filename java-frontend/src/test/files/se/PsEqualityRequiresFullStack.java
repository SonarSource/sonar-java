class A {
  void fp(int param) {
    if(xproc(param) > param) { // Noncompliant False positive
      System.out.println("");
    }
    System.out.println("");
  }

  private int xproc(int param) {
    if(unknownCondition) {
      return -1;
    }
    return param;
  }


}