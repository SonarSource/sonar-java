public class HelloWorld  {

  { // +0 initialization block
  }

  static { // +0 static initialization block
  }

  public void sayHello() { // +1 method
    if (true) { // +1 if-statement
    }

    for (int i = 0; i < 10; i++) { // +1 for-statement
    }

    while (false) { // +1 while-statement
    }

    do { // +1 do-statement
    } while (false);

    switch (ch) {
      case 'a': // +1 case
      case 'b': // +1 case
      default: // +0 default case not counted
        break;
    }

    try {
      throw new RuntimeException();
    } catch (Exception e) {
      return;
    }

    return; // +0 last return-statement
  }

  public void conditional_expression(List<?> list) { // question mark should not be treated as a conditional-expression, so only +1 for method
    int i = list == null ? 0 : false ? 1 : 2; // +2 conditional-expression (was only +1 previously see: SONARJAVA-626)
  }

}

interface Interface {
  void method(); // +0
}

abstract class AbstractClass {
  private int i;
  abstract void method(); // +0
  public int getI(){
    return i; // +1
  }
  public void setI(int i){
    this.i = i; // +1
  }

  public void lambda() {// +1
    Function<String, String> f = s -> { // +1 for the lambda itself
      if(s.isEmpty()) { // +1 for the if
        return s;
      }
      return s;
    };
  }
}

@interface Annotation {
  String value(); // +0
}
