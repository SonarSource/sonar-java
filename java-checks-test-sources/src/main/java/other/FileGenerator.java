package other;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class FileGenerator {

  private static final String FILE_PATH = "src/main/java/other/generated/GeneratedFile.java";
  private static final String FILE_BODY = """
    package other.generated;

    import java.util.Random;

    /**
     * This file has been generated for testing purpose on %s at %s
     */
    public class GeneratedFile {

      private static final Random GENERATOR = new Random();

      // Generated constants
    %s  // dummy methods
      static void doSomething() {
        // do nothing
      }
      static void doSomethingElse() {
        // do nothing
      }
      static boolean test() {
        return GENERATOR.nextBoolean();
      }
      static String doSomethingWith(String s) {
        return s;
      }

      // Generated switch
      void bigSwitch(int i) {
        switch(i) {
    %s      default:
            // do nothing
            break;
        }
      }
    }
    """;
  private static final String SWITCH_BODY_SIMPLE = """
          case %d:
            doSomething();
            break;
    """;
  private static final String SWITCH_BODY_COMPLEX = """
          case %d: {
            if (test()) {
              doSomething();
              break;
            }
            doSomethingElse();
            break;
          }
    """;
  private static final int NUMBER_PSEUDO_NUMBERS_BY_LINE = 20;
  private static final int MAX_STRING_LENGTH = 65535 / 4;
  private static final Random GENERATOR = new Random();
  private static final DateFormat DATE_FORMATER = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
  private static final DateFormat TIME_FORMATER = DateFormat.getTimeInstance(DateFormat.LONG, Locale.US);

  /**
   * Run this method to generate the file "GeneratedFile.java" from package "other.generated"
   * @param args
   */
  public static void main(String[] args) {
    // big
    generate(new Config(20, 10, 1500, 350));
    // small
    // generate(new Config(5, 2, 50, 5));
  }

  private static void generate(Config config) {
    String constants = generateConstants(config);
    String switchBody = generateSwitch(config.numberCasesInSwitch);

    Date now = new Date();
    String content = String.format(FILE_BODY, DATE_FORMATER.format(now), TIME_FORMATER.format(now), constants, switchBody);
    try {
      Files.writeString(new File(FILE_PATH).toPath(), content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Should not have failed generation", e);
    }
  }

  private static String generateConstants(Config config) {
    StringBuilder constantsBuilder = new StringBuilder();
    for (int i = 0; i < config.numberConstants; i++) {
      int numberLines = GENERATOR.nextInt(config.minNumberLineStringConcatenation, config.maxNumberLineStringConcatenation);
      constantsBuilder.append(generateConstant(i, numberLines));
    }
    return constantsBuilder.toString();
  }

  private static String generateConstant(int constantNumber, int numberLines) {
    StringBuilder constantBuilder = new StringBuilder()
      .append("  public static final String CONST_")
      .append(constantNumber)
      .append(" = doSomethingWith(\n");
    boolean maxReached = false;
    int numberChars = 0;
    for (int i = 0; i < numberLines; i++) {
      StringBuilder lineBuilder = new StringBuilder("    \"");
      for (int j = 0; j < NUMBER_PSEUDO_NUMBERS_BY_LINE; j++) {
        lineBuilder
          .append("\\u")
          .append(GENERATOR.nextInt(1000, 9999));
        numberChars++;
        maxReached = numberChars == MAX_STRING_LENGTH;
        if (maxReached) {
          break;
        }
      }
      boolean isLastLine = i >= numberLines - 1;
      String line = lineBuilder
        .append("\"")
        .append((isLastLine || maxReached) ? "" : " +\n")
        .toString();
      constantBuilder.append(line);
      if (maxReached) {
        break;
      }
    }
    return constantBuilder
      .append(");\n\n")
      .toString();
  }

  private static String generateSwitch(int numberCases) {
    StringBuilder casesBuilder = new StringBuilder();
    for (int i = 0; i < numberCases; i++) {
      String base = GENERATOR.nextBoolean() ? SWITCH_BODY_SIMPLE : SWITCH_BODY_COMPLEX;
      casesBuilder.append(String.format(base, i));
    }
    return casesBuilder.toString();
  }

  public static record Config(int numberConstants, int minNumberLineStringConcatenation, int maxNumberLineStringConcatenation, int numberCasesInSwitch) {
    // configuration holder
  }
}
