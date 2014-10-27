public class HelloWorld {

  public void sayHello() {
    while (false) {
    }
  }
  HelloWorld helloWorld = new HelloWorld() {
    @Override
    public void sayHello() {
      while (false){
      }
    }
  };
}
