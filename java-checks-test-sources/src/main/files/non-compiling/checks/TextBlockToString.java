package checks;

public class TextBlockToString {
  
  String string = """
              
  hello
  
  """.toString(); // Noncompliant@-4 [[sc=19;ec=6]] {{there's no need to call "toString()" on a text block.}}

}
