class MyClass {
  public MyClass() {
  }

  public void method() {
  }

  public <T> List<T> genericMethod(T param) {  // counted
    Object o = new MyInterface(){
      void method(){}
      <T> List<T> genericMethod(T param){return null;}
    };
    return null;
  }
}

interface MyInterface {
  void method();  // counted

  <T> List<T> genericMethod(T param);  // counted
}

@interface MyAnnotation {
  String method();  // counted
}

enum MyEnum {
  ONE(){
    int method(){  // counted
      return 1;
    }
  },
  TWO(){
    int method(){  // counted
      return 2;
    }
  };
  abstract int method();  // counted
}