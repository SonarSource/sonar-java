public class HelloWorld {

  public void sayHello() { // Noncompliant [[effortToFix=1]] {{The Cyclomatic Complexity of this method "sayHello" is 2 which is greater than 1 authorized.}}
    while (false) {
    }
  }

}
