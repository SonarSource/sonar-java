package checks;

public class UseIsEmptyToTestEmptinessOfStringBuilderCheckSample {

  void noncompliantStringBuilder() {
    StringBuilder sb = new StringBuilder();

    if ("".equals(sb.toString())) { // Noncompliant {{Replace "equals()" with "isEmpty()".}}
    //  ^^^^^^^^^^^^^^^^^^^^^^^^
      System.out.println("Empty StringBuilder");
    }
    
    if (sb.toString().isEmpty()) { // Noncompliant {{Replace "toString().isEmpty()" with "isEmpty()".}}
    //  ^^^^^^^^^^^^^^^^^^^^^^^
      System.out.println("Empty StringBuilder");
    }

    if (sb.toString().length() == 0) { // Noncompliant {{Replace "toString().length()" with "isEmpty()".}}
      //^^^^^^^^^^^^^^^^^^^^^^
      System.out.println("Empty StringBuilder");
    }

    if (!"".equals(sb.toString())) { // Noncompliant
      System.out.println("Non-empty StringBuilder");
    }

    boolean inExpression = "".equals(sb.toString()); // Noncompliant

    if (sb.toString().equals("")) { // Noncompliant {{Replace "equals()" with "isEmpty()".}}
    //  ^^^^^^^^^^^^^^^^^^^^^^^^
      System.out.println("Empty StringBuilder using equals reversed");
    }
    
    if (!sb.toString().equals("")) { // Noncompliant
      System.out.println("Non-empty StringBuilder using equals reversed");
    }

  }
  
  void noncompliantStringBuffer() {
    StringBuffer sb = new StringBuffer();
    
    if ("".equals(sb.toString())) { // Noncompliant
      System.out.println("Empty StringBuffer");
    }
    
    if (sb.toString().isEmpty()) { // Noncompliant
      System.out.println("Empty StringBuffer");
    }
  }
  
  void compliantStringBuilder() {
    StringBuilder sb = new StringBuilder();
    

    if (sb.isEmpty()) { // Compliant
      System.out.println("Empty StringBuilder");
    }
    

    if (!sb.isEmpty()) { // Compliant
      System.out.println("Non-empty StringBuilder");
    }
    

    boolean expression = sb.isEmpty(); // Compliant

    

    if (sb.length() == 0) { // Compliant
      System.out.println("Empty StringBuilder");
    }


    String sbString = sb.toString();
    if ("".equals(sbString)) { // Compliant, we do not support variables assignment
      System.out.println("Empty string");
    }

    if (sb.isEmpty()) { // Compliant, we do not support variables assignment
      System.out.println("Empty string");
    }

  }
  
  void compliantStringBuffer() {
    StringBuffer sb = new StringBuffer();
    
    if (sb.isEmpty()) { // Compliant
      System.out.println("Empty StringBuffer");
    }
    
    if (!sb.isEmpty()) { // Compliant
      System.out.println("Non-empty StringBuffer");
    }
  }
  

  void complexExpressions() {
    if (getStringBuilder().toString().isEmpty()) { // Noncompliant
      System.out.println("Empty");
    }

    if ("".equals(getStringBuilder().toString())) { // Noncompliant
      System.out.println("Empty");
    }

    StringBuilder sb = new StringBuilder();

    if(sb.append("hello").toString().isEmpty()){ // Noncompliant
      System.out.println("Empty after append");
    }

    if((1==2 ? sb : new StringBuilder()).toString().isEmpty()){ // Noncompliant
      System.out.println("Empty after append");
    }


    
    if (getStringBuilder().isEmpty()) { // Compliant
      System.out.println("Empty");
    }
  }
  
  StringBuilder getStringBuilder() {
    return new StringBuilder("test");
  }
}
