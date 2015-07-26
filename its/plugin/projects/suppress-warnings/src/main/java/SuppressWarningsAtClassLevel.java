@SuppressWarnings("all")
public class SuppressWarningsAtClassLevel {

  final void method(int a) {
    // Squid:S1197
    int var[];
  }

}
