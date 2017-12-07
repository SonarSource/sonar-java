import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class HelloWorld {

  @Autowired// Noncompliant [[secondary=10,13]] {{Remove this annotation and use constructor injection instead.}}
  private String name = null;

  @Autowired
  private String surname = null;

  HelloWorld() {
  }
}

@Controller
public class HelloWorld2 {

  private String name = null;

  @Autowired
  HelloWorld(String name) {
    this.name = name;
  }
}

@Service
public class HelloWorld3 {

  @Autowired// Noncompliant [[secondary=34]] {{Remove this annotation and use constructor injection instead.}}
  private String name = null;

  @Autowired
  private String surname = null;
}
