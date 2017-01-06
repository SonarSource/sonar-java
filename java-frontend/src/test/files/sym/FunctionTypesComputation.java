class FunctionTypesComputation {
  void test(ObservableValue<Boolean> observableValue) {
    observableValue.addListener((a, b, c) -> method(a, b, c));
  }

  private void method(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
  }
}
interface ObservableValue<S> {
  void addListener(ChangeListener<? super S> listener);
}
interface ChangeListener<U> {
  void changed(ObservableValue<? extends U> observable, U oldValue, U newValue);
}