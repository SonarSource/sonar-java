package checks.spring;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

public class SpringComponentSpecializationCheckSample {

  // Service patterns

  @Component // Noncompliant {{Use @Service instead of @Component}}
  public class CustomerServiceImpl {
  }

  @Component // Noncompliant {{Use @Service instead of @Component}}
  public class OrderService {
  }

  @Component // Noncompliant {{Use @Service instead of @Component}}
  public class PaymentServiceFacade {
  }

  // Repository patterns

  @Component // Noncompliant {{Use @Repository instead of @Component}}
  public class ProductRepository {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component}}
  public class UserRepositoryImpl {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component}}
  public class OrderDao {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component}}
  public class CustomerDao {
  }

  // RestController patterns

  @Component // Noncompliant {{Use @RestController instead of @Component}}
  public class FooBarRestController {
  }

  @Component // Noncompliant {{Use @RestController instead of @Component}}
  public class ApiRestController {
  }

  @Component // Noncompliant {{Use @RestController instead of @Component}}
  public class UserRestControllerImpl {
  }

  // Controller patterns

  @Component // Noncompliant {{Use @Controller instead of @Component}}
  public class HomeController {
  }

  @Component // Noncompliant {{Use @Controller instead of @Component}}
  public class LoginControllerImpl {
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

  @Component // Noncompliant {{Use @Service instead of @Component}}
  public class userservice {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component}}
  public class USERREPOSITORY {
  }

  @Component // Noncompliant {{Use @Controller instead of @Component}}
  public class maincontroller {
  }

  @Component // Noncompliant {{Use @RestController instead of @Component}}
  public class apirestcontroller {
  }

  // Interface patterns

  @Component // Noncompliant {{Use @Repository instead of @Component}}
  public interface UserRepository {
  }

  @Component // Noncompliant {{Use @Service instead of @Component}}
  public interface PaymentService {
  }

  @Component // Noncompliant {{Use @Repository instead of @Component}}
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
