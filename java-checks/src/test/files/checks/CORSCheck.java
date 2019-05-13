import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.cors.CorsConfiguration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class A {

  // === Java Servlet ===
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setHeader("Content-Type", "text/plain; charset=utf-8");
    resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8080"); // Noncompliant [[sc=5;ec=19]]
    resp.setHeader("Access-Control-Allow-Credentials", "true"); // Noncompliant [[sc=5;ec=19]]
    resp.setHeader("Access-Control-Allow-Methods", "GET"); // Noncompliant
    resp.getWriter().write("response");
  }
  // === Spring MVC Controller annotation ===
  @CrossOrigin(origins = "http://domain1.com") // Noncompliant [[sc=4;ec=15]] {{Make sure that enabling CORS is safe here.}}
  @RequestMapping("")
  public class TestController {
    public String home(ModelMap model) {
      model.addAttribute("message", "ok ");
      return "view";
    }

    @CrossOrigin(origins = "http://domain2.com") // Noncompliant
    @RequestMapping(value = "/test1")
    public ResponseEntity<String> test1() {
      return ResponseEntity.ok().body("ok");
    }
  }

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOrigin("*"); // Noncompliant
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }

  public CorsFilter corsFilter2() {
    CorsConfiguration config = new CorsConfiguration();
    config.applyPermitDefaultValues(); // Noncompliant
    return new CorsFilter(source);
  }

  public CorsFilter corsFilter3() {
    // test that cut of the visit is necessary
    class Local {
      public CorsFilter corsFilter3() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*"); // Noncompliant [[secondary=67,68]]
        config.applyPermitDefaultValues();
        config.applyPermitDefaultValues();
        config.addAllowedOrigin("*"); // Noncompliant [[secondary=67,68]]
        return new CorsFilter(source);
      }
    }
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*"); // Noncompliant [[secondary=75,76]]
    config.applyPermitDefaultValues();
    config.applyPermitDefaultValues();
    config.addAllowedOrigin("*"); // Noncompliant [[secondary=75,76]]
    return new CorsFilter(source);
  }

}
