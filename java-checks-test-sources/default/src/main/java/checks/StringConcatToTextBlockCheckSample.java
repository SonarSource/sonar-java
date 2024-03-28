package checks;

public class StringConcatToTextBlockCheckSample {
  
  private static final int SOME_INT = 5;
  
  public static void main(String[] args) {

    String s = "<html>\n" + // Noncompliant [[sc=16;ec=25]] {{Replace this String concatenation with Text block.}}
               "    <body>\n" +
               "        <tag>\n" +
               "        </tag>\n" +
               "    </body>\n" +
               "</html>";

    String s333 = "<html>\\n" + // Compliant
               "    <body>\\n" +
               "        <tag>\\n" +
               "        </tag>\\n" +
               "    </body>\\n" +
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

    String s4 = "<html>" + // Compliant, only 2 lines in output
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

    String s6 = "<html>\n" + // Compliant, constant inside
                "    <body>" +
                "        <tag>\n" +
                SOME_INT +
                "        </tag>\n" +
                "    </body>\n" +
                "</html>";
    
    System.out.println(SOME_INT);

    System.out.println("\\n-" + // Compliant, no EOL here
      "\\n-\\n");

    System.out.println("-\n-\n-\n-\n-" + // Compliant, too small lines
      "\n-\n");

    System.out.println("<html>\n" + // Noncompliant
      "    <body>\n" +
      (
        "        <tag>\n" + // Compliant, already reported
          "          xxx\n" +
          "        </tag>\n"
      ) +
      "    </body>\n" +
      "</html>");

    String e = "aaaaaabcdeeeeeeeeeef\n" + "abcdef\n"; // Noncompliant
    String f = "aaaaaaaaaaaaaaabcdef\nabc" + "def\n"; // Noncompliant
    String h = "123456\n1234567777777777777777777777\n" + "\n"; // Noncompliant
    String g = "1234567\n123456777777777777777777777\n" + "\n"; // Noncompliant

  }
}
