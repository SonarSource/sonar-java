import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import javax.inject.Inject;
import javax.annotation.Resource;

@Controller
public class HelloWorld {

  private String name = null; // Noncompliant [[sc=18;ec=22]] {{Annotate this member with "@Autowired", "@Resource", "@Inject", or "@Value", or remove it.}}
  public String address = null; // Noncompliant [[sc=17;ec=24]] {{Annotate this member with "@Autowired", "@Resource", "@Inject", or "@Value", or remove it.}}
  String phone = null; // Noncompliant [[sc=10;ec=15]] {{Annotate this member with "@Autowired", "@Resource", "@Inject", or "@Value", or remove it.}}

  @Autowired 
  String email = null; // Compliant

  @Resource
  String email2 = null; // Compliant

  @Inject
  String email3 = null; // Compliant

  @Value("${some.config.key}")
  String someConfigKey; // Compliant

  private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class); // Compliant
  void someMethod(){}
}

@Service
class ServiceHelloWorld {
  protected String name = null; // Noncompliant [[sc=20;ec=24]] {{Annotate this member with "@Autowired", "@Resource", "@Inject", or "@Value", or remove it.}}
}

@Repository
class RepositoryHelloWorld {
  protected String name = null; // Noncompliant [[sc=20;ec=24]] {{Annotate this member with "@Autowired", "@Resource", "@Inject", or "@Value", or remove it.}}
}

class NonSpringComponentClazz {
  private String name = null; // Compliant
  public String address = null; // Compliant
  String phone = null; // Compliant
  
  @Autowired 
  String email = null; // Compliant
}
@Service
class UniqueConstructor {
  private final String name;

  public UniqueConstructor(String name) {
    this.name = name;
  }
}

@Service
class UniqueConstructor2 {
  private String name; // Noncompliant
  private String address;

  public UniqueConstructor2(String address) {
    // not setting the name field
    this.address = address;
  }
}

@Service
class DualConstructor {
  private final String name; // Noncompliant
  private final String address; // Noncompliant

  public DualConstructor(String name) {
    this.name = name;
  }
  public DualConstructor(String name, String address) {
    this.name = name;
    this.address = address;
  }
}

@Controller
class ConstructorInjection {
  private String env;  // Compliant
  private String yyyAdaptor; // Compliant
  private String jaxbContext; // Noncompliant - not used in @Autowired constructor

  public ConstructorInjection(String env, String yyyAdaptor, String jaxbContext) {
    this.env = env;
    this.yyyAdaptor = yyyAdaptor;
    this.jaxbContext = jaxbContext;
  }

  @Autowired
  public ConstructorInjection(String env, String yyyAdaptor) {
    this.env = env;
    this.yyyAdaptor = yyyAdaptor;
  }
}

