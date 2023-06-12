package filters;

import javax.servlet.http.HttpServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class SpringFilter {

  static class S107 {

    S107(String p1, String p2, String p3, String p4) {} // NoIssue

    S107(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // WithIssue

    void control(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // WithIssue

    @Component
    class SpringComponent{
      public SpringComponent(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue
    }

    @Configuration
    class SpringConfiguration{
      public SpringConfiguration(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue
    }

    @Service
    class SpringService {
      public SpringService(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue
    }

    @Repository
    class SpringRepository{
      public SpringRepository(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue
    }

    @Bean
    public void bean(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @Autowired
    void autowired(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @RequestMapping
    void requestMapping(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @GetMapping
    void getMapping(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @PostMapping
    void postMapping(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @PutMapping
    void putMapping(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @DeleteMapping
    void deleteMapping(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue

    @PatchMapping
    void patchMapping(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // NoIssue
  }

  static class S1185 {
    abstract static class S1185_1 {
      void bar() {}
    }

    @Transactional
    static class S1185_filtered extends S1185_1 {
      @Override
      void bar() { // NoIssue
        super.bar();
      }
    }

    static class S1185_ok extends S1185_1 {
      @Override
      void bar() { // WithIssue
        super.bar();
      }
    }
  }

  static class S1258 {
    static class S2158_1 { // NoIssue
      @Autowired
      private Object myService;
    }

    static class S1258_2 { // NoIssue
      @Autowired
      private Object myService;
      private Object myService2;
    }

    static class S1258_3 { // WithIssue

      private Object myService;
    }
  }

  static class S2226 {
    static class S2226_1 extends HttpServlet {
      S2226_1() {} // kills S1258

      @Autowired
      private javax.sql.DataSource myDB; // NoIssue
    }

    static class S2226_2 {
      S2226_2() {
      } // kills S1258

      private javax.sql.DataSource myDB; // WithIssue
    }
  }
}
