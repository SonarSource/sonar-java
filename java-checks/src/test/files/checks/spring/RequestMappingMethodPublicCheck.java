import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HelloWorld {

  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { // Compliant
  }

  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { // Noncompliant [[sc=18;ec=23]] {{Make this method "public".}}
  }

  public String publicMethodNoAnnotation() {
  }

  private String privateMethodNoAnnotation() {
  }

}

class NonSpringComponentClazz {
  @RequestMapping(value = "/greet", method = GET)
  private String greet(String greetee) { // Compliant
  }

  @RequestMapping(value = "/hello", method = RequestMethod.GET)
  public String hello(String greetee) { // Compliant
  }
}