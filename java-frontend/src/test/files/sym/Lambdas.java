import java.util.Stream;

class A {
  void foo(){
    List<Object> objs = Lists.asList(new Object());
    objs.stream().map(o -> o.toString());
    Stream<Object> s = objs.stream();
    s.map(v -> v.toString());
    s.map(v -> v.toString());
    bar(t -> t + "");
  }

  F bar(F f){
    return s -> this.toString() + s;
  }

  F2 qix() {
    return s -> { return s -> s;};
  }

  F field = s -> s;

  void fun() {
    field = s -> s;
    B b = new B(s->s);
  }

  F cond(boolean a) {
    return a ? s->"" : s->s;
  }
}


class B {
  B(F f){}
}

interface F {
  String apply(String s);
}
interface F2 {
  F apply(String s);
}