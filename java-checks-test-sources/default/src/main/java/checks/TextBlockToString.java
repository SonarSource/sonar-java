package checks;

public class TextBlockToString {

  String string = """
              
  hello
  
  """.toString(); // Noncompliant@-4 {{there's no need to call "toString()" on a text block.}} [[quickfixes=qf1]]
//^[sc=19;ec=6;sl=5;el=9]
  // fix@qf1 {{Remove "toString()"}}
  // edit@qf1 [[sl=9;sc=6;el=9;ec=17]] {{}}

}
