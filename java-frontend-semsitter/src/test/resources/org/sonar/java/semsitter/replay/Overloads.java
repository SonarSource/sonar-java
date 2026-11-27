import java.util.List;

class Overloads {

  void call(int value) {
  }

  void call(long value) {
  }

  void call(double value) {
  }

  void call(Integer value) {
  }

  void call(Object value) {
  }

  void call(String value) {
  }

  void call(String first, Object... rest) {
  }

  void call(int first, int second) {
  }

  void call(List<String> values) {
  }

  void calls() {
    call(1);
    call(1L);
    call(1.5f);
    call('c');
    call((short) 2);
    call(Integer.valueOf(3));
    call("text");
    call(new Object());
    call("a", 1, 2);
    call("a", "b");
    call(1, 2);
    call(List.of("x"));
    call((Object) "cast");
    call(null, null);
    StringBuilder builder = new StringBuilder();
    builder.append(1).append('c').append("s").append(2.0).append(true).append(new char[] {'a'});
    System.out.println(1);
    System.out.println("text");
    System.out.println('c');
    System.out.println(new Object());
    String.format("%s %d", "a", 1);
    Math.max(1, 2L);
    Math.abs(-1.5);
  }
}
