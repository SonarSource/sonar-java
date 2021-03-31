package checks;

class AnonymousClassShouldBeLambdaCheck_java7 {

  interface Handler {
    String handle();
  }

  class A {
    void toto() {
      new Handler() { // Compliant - lambdas are not existing in java 7
        @Override
        public String handle() {
          return "handled";
        }
      }.handle();
    }
  }
}
