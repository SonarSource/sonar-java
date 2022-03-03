package checks;


public class UnusedPrivateClass {

  UnknownFunction<String, String> f() {
    return s -> s  + new Used().toString();
  }

  private class Used {} // Compliant, even if the usage in the previous method is not resolved

}
