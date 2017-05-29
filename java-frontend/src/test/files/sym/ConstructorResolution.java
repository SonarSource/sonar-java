class ParentClass {
  public ParentClass(String s) { }
}

class ChildClass extends ParentClass {
  public ChildClass(Object o) { }

  void foo() {
    new ChildClass("hello world");
  }
}
