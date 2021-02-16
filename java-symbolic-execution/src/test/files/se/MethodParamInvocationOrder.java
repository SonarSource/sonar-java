class MethodParamInvocationOrder {
  private void foo(Object p) {
    if(p == null) {
      MethodParamInvocationOrder
        .method(p.toString()) // Noncompliant
        .method2(p.hashCode());
    }
  }

  static MethodParamInvocationOrder method(String s) {
    return new MethodParamInvocationOrder();
  }
  static MethodParamInvocationOrder method2(String s) {
    return new MethodParamInvocationOrder();
  }
}