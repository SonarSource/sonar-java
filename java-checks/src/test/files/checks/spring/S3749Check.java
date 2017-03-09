import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HelloWorld {

  private String name = null; // Noncompliant [[sc=18;ec=22]] {{Make this member @Autowired or remove it.}}
  public String address = null; // Noncompliant [[sc=17;ec=24]] {{Make this member @Autowired or remove it.}}
  String phone = null; // Noncompliant [[sc=10;ec=15]] {{Make this member @Autowired or remove it.}}

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class); // Compliant

  @RequestMapping(value = "/greet", method = GET)
  public String greet(String greetee) {

    if (greetee != null) {
      this.name = greetee;
    }

    return "Hello " + this.name; // if greetee is null, you see the previous user's data
  }
}

@Service
class ServiceHelloWorld {
  protected String name = null; // Noncompliant [[sc=20;ec=24]] {{Make this member @Autowired or remove it.}}
}

@Repository
class RepositoryHelloWorld {
  protected String name = null; // Noncompliant [[sc=20;ec=24]] {{Make this member @Autowired or remove it.}}
}