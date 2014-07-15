class A {
  private void f(Throwable e) {
    e.printStackTrace(); // Non-Compliant
    e.printStackTrace(System.out); // Non-Compliant
    e.getMessage(); // Compliant
    new java.lang.Throwable().printStackTrace(); // Non-Compliant false negative
    e.printStackTrace[0]; // Compliant
  }
  void fun(MyException e) {
    e.printStackTrace(); //Non-Compliant
  }
  void fun(CustomException e) {
    e.printStackTrace(); //Compliant
    A.CustomException.printStackTrace(); //compliant
  }

  static class CustomException {
    void printStackTrace(Throwable e){

    }
    static void printStackTrace() {}
  }
  static class MyException extends Throwable {
    @Override
    void printStackTrace(){
    }
    void fun(){
      MyException ex = new MyException();
      ex.printStackTrace();
    }
  }
}
