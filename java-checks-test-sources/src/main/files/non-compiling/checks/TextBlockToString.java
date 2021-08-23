package checks;

public class TextBlockToString {
  
  String string = """
              
  hello
  
  """.toString(); // Noncompliant@-4 [[sc=19;ec=6;quickfixes=qf1]] {{there's no need to call "toString()" on a text block.}}
  // fix@qf1 {{Remove "toString()"}}
  // edit@qf1 [[sc=6;ec=17]] {{}}

}
