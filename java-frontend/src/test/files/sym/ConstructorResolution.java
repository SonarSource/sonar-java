class ParentClass {
  public ParentClass(String s) { }
}

class ChildClass extends ParentClass {
  public ChildClass(Object o) { }

  void foo() {
    new ChildClass("hello world");
    java.util.Map<Integer, String> m = new java.util.HashMap<>(someMethod());
  }
}
