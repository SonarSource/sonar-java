package checks;

import java.util.Arrays;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

public class CORSCheckSample {

  private void additional_Spring_5_3_methods() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("*")); // Noncompliant
    configuration.setAllowedOrigins(List.of("*")); // Noncompliant
    configuration.setAllowedOrigins(List.of("com.mycompany")); // Compliant
    configuration.setAllowedOriginPatterns(List.of("*")); // Noncompliant

    CorsRegistration registration = new CorsRegistration("");
    registration.allowedOrigins("*"); // Noncompliant
    registration.allowedOriginPatterns("*"); // Noncompliant
    registration.allowedOriginPatterns("com.mycompany/*"); // Complaint

    CorsRegistry registry = new CorsRegistry();
    registry.addMapping("*"); // Noncompliant
    registry.addMapping("com.mycompany"); // Compliant
    registry.addMapping("com.mycompany/*"); // Complaint
  }

  private void coverage() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("com.mycompany", "*", "com.yourcompany")); // Noncompliant
    configuration.setAllowedOrigins(List.of("com.mycompany", "com.yourcompany")); // Compliant
    configuration.setAllowedOrigins(List.of()); // Compliant
    configuration.setAllowedOrigins(getList()); // Compliant
    var list = List.of("com.mycompany", "*", "com.yourcompany");
    configuration.setAllowedOrigins(list); // Compliant

    CorsRegistration registration = new CorsRegistration("");
    registration.allowedOrigins("com.mycompany", "*", "com.yourcompany"); // Noncompliant
    registration.allowedOrigins("com.mycompany", "com.yourcompany"); // Compliant
    registration.allowedOrigins(); // Compliant
  }

  private static List<String> getList() {
    return List.of("com.mycompany", "*", "com.yourcompany");
  }
}
