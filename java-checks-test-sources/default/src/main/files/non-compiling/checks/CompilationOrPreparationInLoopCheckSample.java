package checks;

import java.util.List;
import java.util.regex.Pattern;

class CompilationOrPreparationInLoopCheckSampleNonCompiling {
  void test(List<String> inputs) {
    for (String input : inputs) {
      Pattern.compile(unknownVar).matcher(input).find(); // Compliant - unresolved symbol is not a variable symbol
    }
  }
}
