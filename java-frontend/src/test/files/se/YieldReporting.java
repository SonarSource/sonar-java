class YieldReporting {

  // this is just to provide a yield for MIT in #test()
  private Object resultIsNull() {
    return null;
  }

  void test() {
    resultIsNull();
    // here yield stored as a field in checkerDispatcher should be null, because that yield is only used on transitions from the resultIsNull() MIT
  }
}
