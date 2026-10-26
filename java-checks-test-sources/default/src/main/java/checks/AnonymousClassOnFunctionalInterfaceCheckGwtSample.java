package checks;

class AnonymousClassOnFunctionalInterfaceCheckGwtSample {

  interface ClickHandler {
    void onClick(String event);
  }

  void addClickHandler(ClickHandler handler) {
  }

  void register() {
    addClickHandler(new ClickHandler() { // Noncompliant {{Make this anonymous inner class a lambda}}
      @Override
      public void onClick(String event) {
      }
    });
  }
}
