public class SuppressWarningsAtMethodLevel {

  @SuppressWarnings("all")
  final void method(int a) {
    // Squid:S1197
    int var[];
  }

  final void method2(int a) {
    int var[];
  }

}
