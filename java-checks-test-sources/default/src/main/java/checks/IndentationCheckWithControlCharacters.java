package checks;

public class IndentationCheckWithControlCharacters {
  String a = "'' next line";            // U+0085
  String b = "' ' line separator";      // U+2028

  public void thisMethodWillCrash() {
    return;
  }
  String c = "' ' paragraph separator"; // U+2029
}
