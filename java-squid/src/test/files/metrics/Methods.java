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

enum MyEnum {
  ONE(){
    int method(){
      return 1;
    }
  },
  TWO(){
    int method(){
      return 2;
    }
  };
  abstract int method();
}