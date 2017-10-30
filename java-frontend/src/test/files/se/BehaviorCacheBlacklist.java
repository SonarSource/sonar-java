import java.util.Optional;

class A {
  void test(String s) {
    ClassLoader cl = s.getClass().getClassLoader();
    cl.toString();
    Class.forName("blabal");
    wait();
  }

  void optional(Optional<Integer> op) {
    int i = op.isPresent ? op.get() : 0;
  }
}
