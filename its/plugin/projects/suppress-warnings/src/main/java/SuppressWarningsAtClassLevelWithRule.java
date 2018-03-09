@SuppressWarnings("squid:S1197")
public class SuppressWarningsAtClassLevelWithRule {
  int var[];
  @SuppressWarnings({"java-extension:example"})
  final void method(int a) {
    // Squid:S1197
    int var[];
  }

}
