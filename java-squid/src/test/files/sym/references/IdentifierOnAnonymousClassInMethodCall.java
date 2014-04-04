class IdentifierOnAnonymousClassInMethodCall {

  void method() {
    new Callable<Boolean>() {
      public Boolean call() throws Exception {
        boolean rv = isEnabled();
        if (rv) {
          setEnabled(false);
        }
        return rv;
      }
    }.call();
  }
}