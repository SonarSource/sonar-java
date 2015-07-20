public class HelloWorld { // counted
  void method(){
    class LocalInnerClass{ // counted
    }
    AbstractMap anonymousMap = new AbstractMap() {
      public Set entrySet() {
        return null;
      }
    };
  }
  static class staticInnerClass{} // counted
  interface innerInterface{}  // counted
}

private @interface annotationType {}  // counted

class AnotherClass { // counted
  class innerClass{ // counted

  }

}

enum Enum { // counted
  A{

  },
  B{

  };
}
