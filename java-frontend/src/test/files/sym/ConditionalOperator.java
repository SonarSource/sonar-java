import java.util.List;
public class App {
  public class Foo<F extends List<Object>> {
  }
  public class Bar<G extends List<Object>> extends Foo<G> {
  }
  private <H extends List<Object>> Foo fun() {
    return true ? new Bar() : new Foo<List<Object>>(); // triggers computation of least upper bound
  }
}
