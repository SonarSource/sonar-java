class A {
  void foo(){
    List<Object> objs = Lists.asList(new Object());
    objs.stream().map(o -> o.toString());
  }
}