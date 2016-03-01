import java.util.Stream;

class A {
  void foo(){
    List<Object> objs = Lists.asList(new Object());
    objs.stream().map(o -> o.toString());
    Stream<Object> s = objs.stream();
    s.map(v -> v.toString());
    s.map(v -> v.toString());
  }
}