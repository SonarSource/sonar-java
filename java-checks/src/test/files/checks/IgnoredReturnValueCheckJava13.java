class Foo {

  void bar(String s, String cond) {
    String v = switch (cond) {
     case "1" -> "aValue";
     case "2" -> s.toString();
     case "3" -> s.substring(1);
     default -> s.replace('a', 'b');
    };
  }
}
