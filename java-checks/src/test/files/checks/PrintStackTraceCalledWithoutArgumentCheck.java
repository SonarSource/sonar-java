import java.lang.reflect.InvocationTargetException;

class A {
  private void f(Throwable e) {
    e.printStackTrace(); // Noncompliant {{Use a logger to log this exception.}}
    e.printStackTrace(System.out); // Noncompliant {{Use a logger to log this exception.}}
    e.getMessage(); // Compliant
    new java.lang.Throwable().printStackTrace(); // Noncompliant
    e.printStackTrace[0]; // Compliant
  }
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
    @Override
    void printStackTrace(){
    }
    void fun(){
      MyException ex = new MyException();
      ex.printStackTrace();
    }
  }
}
