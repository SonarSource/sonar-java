import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class HelloWorld {

  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { // Compliant
  }

  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { // Noncompliant [[sc=18;ec=23]] {{Make this method "public".}}
  }

  @GetMapping
  public String a() { }

  @GetMapping
  private String a1() { }  // Noncompliant

  @PostMapping
  public String b() { }

  @PostMapping
  private String b1() { } // Noncompliant

  @PutMapping
  public String c() { }

  @PutMapping
  private String c1() { } // Noncompliant

  @DeleteMapping
  public String d() { }

  @DeleteMapping
  private String d1() { } // Noncompliant

  @PatchMapping
  public String e() { }

  @PatchMapping
  private String e1() { } // Noncompliant

  public String publicMethodNoAnnotation() {
  }

  private String privateMethodNoAnnotation() {
  }

}

@RestController
class Foo {
  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { // Compliant
  }

  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { // Noncompliant [[sc=18;ec=23]] {{Make this method "public".}}
  }

  @GetMapping public String a() { }
  @GetMapping private String a1() { }  // Noncompliant
  @PostMapping public String b() { }
  @PostMapping private String b1() { } // Noncompliant
  @PutMapping public String c() { }
  @PutMapping private String c1() { } // Noncompliant
  @DeleteMapping public String d() { }
  @DeleteMapping private String d1() { } // Noncompliant
  @PatchMapping public String e() { }
  @PatchMapping private String e1() { } // Noncompliant

  public String publicMethodNoAnnotation() { }

  private String privateMethodNoAnnotation() { }

}

class NonSpringComponentClazz {
  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { // Compliant
  }

  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { // Compliant
  }
}
