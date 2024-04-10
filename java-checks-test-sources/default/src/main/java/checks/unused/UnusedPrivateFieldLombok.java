package checks.unused;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class UnusedPrivateFieldLombok {

  @Data
  class LombokDataClass {
    private String data; // Compliant
    public class InnerClass {
      private String innerData; // Noncompliant
      private String usedData; // Compliant
    }
    void foo(){
      var s = new InnerClass().usedData;
    }
  }

  @Getter
  class LombokGetterClass{
    private String data; // Compliant
  }

  @Setter
  class LombokSetterClass{
    private String data; // Compliant
  }

  @AllArgsConstructor
  class AllArgsConstructorClass {
    private String data; // Compliant
  }

}
