import org.springframework.stereotype.Component;

@Component
public class SpringBootAppInDefaultPackage { // Noncompliant
  SpringBootAppInDefaultPackage() {
  }
}
