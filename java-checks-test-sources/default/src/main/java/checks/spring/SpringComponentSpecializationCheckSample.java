package checks.spring;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public class SpringComponentSpecializationCheckSample {

  // Service patterns

  @Component // Noncompliant {{Use @Service instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class CustomerServiceImpl {
  }

  @Component // Noncompliant {{Use @Service instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class OrderService {
  }

  @Component // Noncompliant {{Use @Service instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class PaymentServiceFacade {
  }

  // Repository patterns

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class ProductRepository {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class UserRepositoryImpl {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class OrderDao {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class CustomerDao {
  }

  // RestController patterns - with request mapping methods

  @Component // Noncompliant {{Use @RestController instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class FooBarRestController {
    @GetMapping("/foo")
    public String foo() { return "foo"; }
  }

  @Component // Noncompliant {{Use @RestController instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class ApiRestController {
    @RequestMapping("/api")
    public String api() { return "api"; }
  }

  @Component // Noncompliant {{Use @RestController instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class UserRestControllerImpl {
    @PostMapping("/users")
    public void createUser() { }
  }

  // Controller patterns - with request mapping methods

  @Component // Noncompliant {{Use @Controller instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class HomeController {
    @GetMapping("/home")
    public String home() { return "home"; }
  }

  @Component // Noncompliant {{Use @Controller instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class LoginControllerImpl {
    @PostMapping("/login")
    public String login() { return "login"; }
  }

  // Compliant - Controllers without request mapping methods (FP fix)

  @Component
  public class BatchController {
  }

  @Component
  public class DataProcessingController {
    public void process() { }
  }

  @Component
  public class SchedulerRestController {
    public void runTask() { }
  }

  // Compliant - Controllers implementing non-web framework interfaces

  @Component
  public class StartupController implements ApplicationRunner {
    @Override
    public void run(org.springframework.boot.ApplicationArguments args) { }
  }

  @Component
  public class InitController implements CommandLineRunner {
    @Override
    public void run(String... args) { }
  }

  // Compliant - Redundant annotation: @Component alongside a specialized stereotype

  @Component
  @Service
  public class RedundantServiceAnnotation {
  }

  @Component
  @Controller
  public class RedundantControllerAnnotation {
  }

  @Component
  @RestController
  public class RedundantRestControllerAnnotation {
  }

  @Component
  @Repository
  public class RedundantRepositoryAnnotation {
  }

  // Compliant - Correct annotations used

  @Service
  public class CustomerServiceImplCorrect {
  }

  @Repository
  public class ProductRepositoryCorrect {
  }

  @RestController
  public class FooBarRestControllerCorrect {
  }

  @Controller
  public class HomeControllerCorrect {
  }

  // Generic component names

  @Component
  public class SomeOtherComponent {
  }

  @Component
  public class UtilityHelper {
  }

  @Component
  public class CacheManager {
  }

  @Component
  public class DataProcessor {
  }

  // No annotation

  public class PlainClass {
  }

  // Case variations

  @Component // Noncompliant {{Use @Service instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class userservice {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public class USERREPOSITORY {
  }

  @Component
  public class maincontroller {
    // Compliant - no request mapping methods
  }

  @Component
  public class apirestcontroller {
    // Compliant - no request mapping methods
  }

  // Interface patterns

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public interface UserRepository {
  }

  @Component // Noncompliant {{Use @Service instead of @Component, or rename this type if the @Component annotation is intentional}}
  public interface PaymentService {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component, or rename this type if the @Component annotation is intentional}}
  public interface ProductDao {
  }

  // Compliant interfaces - correct annotations used

  @Repository
  public interface CategoryRepository {
  }

  @Service
  public interface NotificationService {
  }

  // Compliant interfaces - generic names

  @Component
  public interface EventListener {
  }
}
