package filters;

import java.util.List;

public class SpringFilter {

  static class S100 implements Repository<Person, Long> {
    static class S100_1  {
      // FP in case of missing semantic - 'Repository' is not resolved
      List<Person> findByAddress_ZipCode(ZipCode zipCode) { // WithIssue
        return List.of();
      }
    }

    interface Person {}
    interface ZipCode {}
  }

  static class S1185 {
    abstract static class S1185_1 {
      void bar() {}
    }

    // not resolved
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


  static class S2226 {
    static class S2226_1 extends HttpServlet {
      S2226_1() {} // kills S1258

      // not resolved
      @Autowired
      private javax.sql.DataSource myDB; // NoIssue

      @org.springframework.beans.factory.annotation.Autowired
      private javax.sql.DataSource myDB2; // NoIssue
    }
  }
}
