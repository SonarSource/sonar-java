package checks;

import java.util.Iterator;
import java.util.List;

public class StringBufferAndBuilderConcatenationCheckSample {
  public String appendCorrect() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append("text1")
        .append("text2");
    }
    return sb.toString();
  }

  public String noLoop() {
    StringBuffer sb = new StringBuffer();
    sb.append("text1" + "text2");
    return sb.toString();
  }

  public String nestedLoops() {
    StringBuffer sbOutside = new StringBuffer();
    StringBuffer sbInside1 = new StringBuffer();
    StringBuffer sbInside2 = new StringBuffer();
    StringBuffer sbInside21 = new StringBuffer();

    sbInside1.append("text1" + "text2");
    for (int i = 0; i < 5; i++) {
      sbInside1.append("i = " + i); // Noncompliant
      for (int j = 0; j < 5; j++) {
        sbInside2.append("j = " + j); // Noncompliant
        if (i == j) {
          sbInside21.append("i eq j = " + i); // Noncompliant
        }
      }
    }

    return new StringBuilder()
      .append(sbOutside)
      .append(sbInside1)
      .append(sbInside2)
      .append(sbInside21)
      .toString();
  }

  public String loopWhile() {
    StringBuffer sb = new StringBuffer();
    int i = 0;
    while (i < 10) {
      sb.append("text1" + "text2"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfWhile]]
//              ^^^^^^^^^^^^^^^^^
//              fix@qfWhile {{Call "append" multiple times.}}
//              edit@qfWhile [[sc=16;ec=35]] {{("text1").append("text2")}}
      i++;
    }
    return sb.toString();
  }

  public String loopDoWhile() {
    StringBuffer sb = new StringBuffer();
    int i = 0;
    do {
      sb.append("text1" + "text2"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfDoWhile]]
//              ^^^^^^^^^^^^^^^^^
//              fix@qfDoWhile {{Call "append" multiple times.}}
//              edit@qfDoWhile [[sc=16;ec=35]] {{("text1").append("text2")}}
      i++;
    } while (i < 10);
    return sb.toString();
  }

  public String loopForeach(List<String> items) {
    StringBuffer sb = new StringBuffer();
    for (String item : items) {
      sb.append("<" + item + ">\n"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfForeach]]
//              ^^^^^^^^^^^^^^^^^^
//              fix@qfForeach {{Call "append" multiple times.}}
//              edit@qfForeach [[sc=16;ec=36]] {{("<").append(item).append(">\n")}}
    }
    return sb.toString();
  }

  public String functionalForeach(List<String> items) {
    // FN: not implemented
    StringBuffer sb = new StringBuffer();
    items.forEach(item -> sb.append("<" + item + ">\n"));
    return sb.toString();
  }

  public String functionalStreamForeach(List<String> items) {
    // FN: not implemented
    StringBuffer sb = new StringBuffer();
    items
      .stream()
      .forEach(item -> sb.append("<" + item + ">\n"));
    return sb.toString();
  }

  public String loopForIf() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      if (i % 2 == 0) {
        sb.append("number: " + i); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfForIf]]
//                ^^^^^^^^^^^^^^
//              fix@qfForIf {{Call "append" multiple times.}}
//              edit@qfForIf [[sc=18;ec=34]] {{("number: ").append(i)}}
      }
    }
    return sb.toString();
  }

  public String concatThree() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append("text1" + "text2" + "text3"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfThree]]
//              ^^^^^^^^^^^^^^^^^^^^^^^^^^^
//              fix@qfThree {{Call "append" multiple times.}}
//              edit@qfThree [[sc=16;ec=45]] {{("text1").append("text2").append("text3")}}
    }
    return sb.toString();
  }

  public String concatIntNoncompliant() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append("text" + 1); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfInt]]
//              ^^^^^^^^^^
//              fix@qfInt {{Call "append" multiple times.}}
//              edit@qfInt [[sc=16;ec=28]] {{("text").append(1)}}
    }
    return sb.toString();
  }

  public String concatIntCompliant() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append(1 + 1);
    }
    return sb.toString();
  }

  public String complexArgumentList() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append("two " + (1 + 1)); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfComplex]]
//              ^^^^^^^^^^^^^^^^
//              fix@qfComplex {{Call "append" multiple times.}}
//              edit@qfComplex [[sc=16;ec=34]] {{("two ").append(1 + 1)}}
    }
    return sb.toString();
  }

  public void chained() {
    for (int i = 0; i < 10; i++) {
      String s = new StringBuilder()
        .append("text1" + "text2") // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfChained]]
//              ^^^^^^^^^^^^^^^^^
//              fix@qfChained {{Call "append" multiple times.}}
//              edit@qfChained [[sc=16;ec=35]] {{("text1").append("text2")}}
        .toString();
      System.out.println(s);
    }
  }

  public void parameter(StringBuilder passed) {
    for (int i = 0; i < 10; i++) {
      passed.append("hello " + "hello");  // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfPassed]]
//                  ^^^^^^^^^^^^^^^^^^
//                  fix@qfPassed {{Call "append" multiple times.}}
//                  edit@qfPassed [[sc=20;ec=40]] {{("hello ").append("hello")}}
    }
  }

  public String call(Iterator<String> iter) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append("<" + iter.next() + "/>"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfCall]]
//              ^^^^^^^^^^^^^^^^^^^^^^^^
//              fix@qfCall {{Call "append" multiple times.}}
//              edit@qfCall [[sc=16;ec=42]] {{("<").append(iter.next()).append("/>")}}
    }
    return sb.toString();
  }

  public String nestedCall(String s) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append(s.toLowerCase());
    }
    return sb.toString();
  }

  public String stringBufferAppendSimple() {
    StringBuffer stringBuffer = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      stringBuffer.append("text1")
        .append("text2");
    }
    return stringBuffer.toString();
  }

  public String stringBufferConcatSimple() {
    StringBuffer stringBuffer = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      stringBuffer.append("text1" + "text2"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qfBuffer]]
//                        ^^^^^^^^^^^^^^^^^
//                        fix@qfBuffer {{Call "append" multiple times.}}
//                        edit@qfBuffer [[sc=26;ec=45]] {{("text1").append("text2")}}
    }
    return stringBuffer.toString();
  }

  public String appendWithStartEnd() {
    // Breaking this up would require changing the second and third argument.
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      sb.append("text1" + "text2", 2, 3);
    }
    return sb.toString();
  }

  public String crossesMethodBoundary() {
    // FN: we do not analyze across method calls
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 10; i++) {
      anotherMethod(sb);
    }
    return sb.toString();
  }

  private void anotherMethod(StringBuffer sb) {
    sb.append("text1" + "text2");
  }
}
