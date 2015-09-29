public class HelloWorld { // Noncompliant {{The Cyclomatic Complexity of this class is 4 which is greater than 1 authorized.}}

  public void sayHello() {
    while (false) {
    }
  }
  HelloWorld helloWorld = new HelloWorld() { // Noncompliant
    @Override
    public void sayHello() {
      while (false){
      }
    }
  };
}
