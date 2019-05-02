import java.lang.reflect.InvocationTargetException;

class A {
  private void f(Throwable e, MyException e1) {
    e.printStackTrace(); // Noncompliant [[sc=7;ec=22]] {{Use a logger to log this exception.}}
    e.printStackTrace(System.out); // Compliant - forcing the stream
    e.getMessage(); // Compliant
    new java.lang.Throwable().printStackTrace(); // Noncompliant
    String s = e1.printStackTrace[0]; // Compliant
    printStackTrace();
  }
  void printStackTrace() {}
  void fun(MyException e) {
    e.printStackTrace(); // Noncompliant
  }
  void fun(CustomException e) {
    e.printStackTrace(); //Compliant : e is not extending Throwable
    A.CustomException.printStackTrace(); //compliant : CustomException is not extending Throwable
  }

  void fun(InvocationTargetException ite) {
    ite.getTargetException().printStackTrace(); // Noncompliant
  }

  static class CustomException {
    void printStackTrace(Throwable e){

    }
    static void printStackTrace() {}
  }
  static class MyException extends Throwable {
    public String[] printStackTrace;

    @Override
    void printStackTrace(){
    }
    void fun(){
      MyException ex = new MyException();
      ex.printStackTrace();
    }
  }
}
