package checks;

public class StringConcatToTextBlockCheck {
  
  private static final int SOME_INT = 5;
  
  public static void main(String[] args) {

    String s = "<html>\n" + // Noncompliant [[sc=16;ec=25]] {{Replace this String concatenation with Text block.}}
               "    <body>\n" +
               "        <tag>\n" +
               "        </tag>\n" +
               "    </body>\n" +
               "</html>";

    String s1 = "<html>\n" + // Noncompliant [[sc=17;ec=25]] {{Replace this String concatenation with Text block.}}
               "    <body>" +
               "        <tag>" +
               "        </tag>" +
               "    </body>\n" +
               "</html>";

    String s2 = "<html>" + // Noncompliant [[sc=17;ec=25]] {{Replace this String concatenation with Text block.}}
               "    <body>\n" +  
               "        <tag>" +
               "        </tag>" +
               "    </body>\n" +
               "</html>";

    String s3 = "<html>" + // Noncompliant [[sc=17;ec=25]] {{Replace this String concatenation with Text block.}}
               "    <body>\n" +  
               "        <tag>" +
                123 +
               "        </tag>" +
               "    </body>\n" +
               "</html>";

    String s4 = "<html>" + // Compliant, only 2 lines
               "    <body>\n" +  
               "        <tag>" +
               "        </tag>" +
               "    </body>" +
               "</html>";
    
    int i1 = 100 + 250;
    int i2 = 100;

    String s5 = "<html>\n" + // Compliant, variable inside
                "    <body>\n" +
                "        <tag>\n" +
                i2 +
                "        </tag>\n" +
                "    </body>\n" +
                "</html>";

    String s6 = "<html>\n" + // Noncompliant, constant inside
                "    <body>\n" +
                "        <tag>\n" +
                SOME_INT +
                "        </tag>\n" +
                "    </body>\n" +
                "</html>";
    
    System.out.println(SOME_INT);
  }
}
