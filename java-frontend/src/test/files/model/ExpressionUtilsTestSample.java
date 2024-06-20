class ExpressionUtilsTestSample {

  private boolean parenthesis(boolean b1, boolean b2) {
    return (((b1 && (b2))));
  }

  private void simpleAssignment() {
    int x;
    x = 14;
    (x) = 14;
    x += 1;

    int[] y = new int[5];
    y[x] = 42;
  }

}
