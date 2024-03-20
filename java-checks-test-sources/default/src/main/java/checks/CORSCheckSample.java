package checks;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class CORSCheckSample {

  private static final String STAR = "*";
  private static final String NOT_STAR = "http://domain2.com";


  // === Java Servlet ===
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setHeader("Content-Type", "text/plain; charset=utf-8");
    resp.setHeader("Access-Control-Allow-Origin", "*"); // Noncompliant [[sc=10;ec=19]]
    // header names are case insensitive. see https://stackoverflow.com/questions/5258977/are-http-headers-case-sensitive/5259004#5259004
    resp.setHeader("Access-control-allow-Origin", "*"); // Noncompliant [[sc=10;ec=19]]
    resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8080"); // Compliant

    resp.setHeader("Access-Control-Allow-Credentials", "true"); // Compliant
    resp.setHeader("Access-Control-Allow-Credentials", "*"); // Compliant
    resp.setHeader("access-control-allow-Methods", "GET"); // Compliant
    resp.setHeader("Access-Control-Allow-Methods", "*"); // Compliant

    resp.addHeader("Content-Type", "text/plain; charset=utf-8");
    resp.addHeader("Access-Control-Allow-Origin", "*"); // Noncompliant [[sc=10;ec=19]]
    resp.addHeader("Access-Control-Allow-Origin", "http://localhost:8080"); // // Compliant
    resp.addHeader("Access-Control-Allow-Credentials", "true"); // Compliant
    resp.addHeader("Access-Control-Allow-Methods", "GET"); // Compliant

    resp.addHeader("Access-control-allow-Origin", null); // Compliant
    resp.addHeader(null, "*"); // Compliant

    resp.getWriter().write("response");
  }

  protected void doGetJakarta(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse resp) {
    resp.setHeader("Access-Control-Allow-Origin", "*"); // Noncompliant [[sc=10;ec=19]]
    resp.setHeader("Access-control-allow-Origin", "*"); // Noncompliant [[sc=10;ec=19]]
    resp.addHeader("Access-Control-Allow-Origin", "*"); // Noncompliant [[sc=10;ec=19]]
  }

  // === Spring MVC Controller annotation ===
  @CrossOrigin(origins = "*") // Noncompliant [[sc=4;ec=15]] {{Make sure that enabling CORS is safe here.}}
  @RequestMapping("")
  public class TestController {
    public String home(ModelMap model) {
      model.addAttribute("message", "ok ");
      return "view";
    }

    @CrossOrigin("*") // Noncompliant
    @RequestMapping(value = "/test1")
    public ResponseEntity<String> test1() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin // Noncompliant
    @RequestMapping(value = "/test2")
    public ResponseEntity<String> test2() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(allowedHeaders = "http://domain2.com") // Noncompliant
    @RequestMapping(value = "/test3")
    public ResponseEntity<String> test3() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin("http://domain2.com") // Compliant, value is an alias for origins
    @RequestMapping(value = "/test4")
    public ResponseEntity<String> test4() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(value = "http://domain2.com") // Compliant, value is an alias for origins
    @RequestMapping(value = "/test5")
    public ResponseEntity<String> test5() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(origins = "http://domain2.com") // Compliant
    @RequestMapping(value = "/test6")
    public ResponseEntity<String> test6() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(origins = {"http://localhost:7777", "http://someserver:8080"}) // Compliant
    @RequestMapping(value = "/test7")
    public ResponseEntity<String> test7() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(origins = {"*"}) // Noncompliant
    @RequestMapping(value = "/test8")
    public ResponseEntity<String> test8() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(origins = {"http://localhost:7777", "*"}) // Noncompliant
    @RequestMapping(value = "/test9")
    public ResponseEntity<String> test9() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(STAR) // Noncompliant
    @RequestMapping(value = "/test10")
    public ResponseEntity<String> test10() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(CORSCheckSample.STAR) // Noncompliant
    @RequestMapping(value = "/test11")
    public ResponseEntity<String> test11() {
      return ResponseEntity.ok().body("ok");
    }

    @CrossOrigin(NOT_STAR) // Compliant
    @RequestMapping(value = "/test12")
    public ResponseEntity<String> test12() {
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
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("http://domain2.com"); // Compliant
    return new CorsFilter(source);
  }

  public CorsFilter corsFilter3() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.applyPermitDefaultValues(); // Noncompliant
    return new CorsFilter(source);
  }

  public CorsFilter corsFilter4() {
    // test that cut of the visit is necessary
    class Local {
      public CorsFilter corsFilter4() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*"); // Noncompliant [[secondary=+1,+2]]
        config.applyPermitDefaultValues();
        config.applyPermitDefaultValues();
        config.addAllowedOrigin("*"); // Noncompliant [[secondary=-2,-1]]
        return new CorsFilter(source);
      }
    }
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*"); // Noncompliant [[secondary=+1,+2]]
    config.applyPermitDefaultValues();
    config.applyPermitDefaultValues();
    config.addAllowedOrigin("*"); // Noncompliant [[secondary=-2,-1]]
    return new CorsFilter(source);
  }

  class S5122_Insecure implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/**")
        .allowedOrigins("*"); // Noncompliant [[sc=10;ec=24]]
    }
  }

  class S5122_Safe implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/**")
        .allowedOrigins("safe.com"); // Compliant
    }
  }

}
