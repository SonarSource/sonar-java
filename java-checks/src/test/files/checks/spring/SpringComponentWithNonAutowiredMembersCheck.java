import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import javax.inject.Inject;
import javax.annotation.Resource

@Controller
public class HelloWorld {

  private String name = null; // Noncompliant [[sc=18;ec=22]] {{Make this member injected via Annotation (@Autowired, @Resource) or remove it.}}
  public String address = null; // Noncompliant [[sc=17;ec=24]] {{Make this member injected via Annotation (@Autowired, @Resource) or remove it.}}
  String phone = null; // Noncompliant [[sc=10;ec=15]] {{Make this member injected via Annotation (@Autowired, @Resource) or remove it.}}

  @Autowired 
  String email = null; // Compliant

  @Resource
  String email2 = null; // Compliant
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class); // Compliant
}

@Service
class ServiceHelloWorld {
  protected String name = null; // Noncompliant [[sc=20;ec=24]] {{Make this member injected via Annotation (@Autowired, @Resource) or remove it.}}
}

@Repository
class RepositoryHelloWorld {
  protected String name = null; // Noncompliant [[sc=20;ec=24]] {{Make this member injected via Annotation (@Autowired, @Resource) or remove it.}}
}

class NonSpringComponentClazz {
  private String name = null; // Compliant
  public String address = null; // Compliant
  String phone = null; // Compliant
  
  @Autowired 
  String email = null; // Compliant
}
