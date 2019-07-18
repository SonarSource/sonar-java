import javax.persistence.Entity;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Entity
public class Foo {
}

@Document
public class Doc {
}

@org.springframework.data.elasticsearch.annotations.Document
public class ElasticsearchDoc {

}

public class Bar {
}

@Component
public class Baz {
}

@Controller
class FooController {

  @RequestMapping(path = "/foo", method = RequestMethod.POST)
  public void foo1(Foo foo) { // Noncompliant [[sc=24;ec=27]] {{Replace this persistent entity with a simple POJO or DTO object.}}
  }

  @GetMapping
  public void foo2(Foo foo) { // Noncompliant
  }

  @PostMapping
  public void foo3(Foo foo) { // Noncompliant
  }

  @PostMapping
  public void foo31(@PathVariable("id") Foo foo) { // Compliant, lookup will be done via id, object cannot be forged on client side.
  }

  @RequestMapping
  public void foo32(
    @PathVariable Foo foo, // Compliant
    Doc doc) { // Noncompliant
  }

  @PostMapping
  public void foo33(@PathVariable final Foo foo) { // Compliant
  }

  @PutMapping
  public void foo4(ElasticsearchDoc doc) { // Noncompliant
  }

  @DeleteMapping
  public void foo5(Foo foo) { // Noncompliant
  }

  @PatchMapping
  public void foo6(Doc Doc) { // Noncompliant
  }

  @RequestMapping
  public void foo7(
    String x,
    Foo foo, // Noncompliant
    Doc doc) { // Noncompliant
  }

  @PostMapping
  public Foo ok1(String s) {
    Foo foo = new Foo();
    return foo; // it is ok to return
  }

  public void ok2(Foo foo) {
  }

  public void ok3(Doc doc) {
  }

  @PostMapping
  public void ok4(Bar bar, Baz baz) {
  }

  @DeleteMapping
  public void ok5(Bar bar) {
  }
}
