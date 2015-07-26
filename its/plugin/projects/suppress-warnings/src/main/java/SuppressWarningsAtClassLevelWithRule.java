@SuppressWarnings("squid:S1197")
public class SuppressWarningsAtClassLevelWithRule {
  int var[];
  final void method(int a) {
    // Squid:S1197
    int var[];
  }

}
