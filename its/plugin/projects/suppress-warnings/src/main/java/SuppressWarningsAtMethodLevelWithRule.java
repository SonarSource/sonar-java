public class SuppressWarningsAtMethodLevelWithRule {

  int var[];
  @SuppressWarnings({"squid:S1197","java-extension:example"})
  final void method(int a) {
    // Squid:S1197
    int var[];
  }
}