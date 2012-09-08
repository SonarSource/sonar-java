class MyClass {
  public MyClass() {
  }

  public void method() {
  }

  public <T> List<T> genericMethod(T param) {
    return null;
  }
}

interface MyInterface {
  void method();

  <T> List<T> genericMethod(T param);
}

@interface MyAnnotation {
  String method();
}
