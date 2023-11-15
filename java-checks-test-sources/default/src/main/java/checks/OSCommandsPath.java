package checks;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSCommandsPath {
  private static final String NON_COMPLIANT_COMMAND = "make";
  private static final String NON_COMPLIANT_COMMAND_UNIX_PARENT = "m../ake";
  private static final String NON_COMPLIANT_COMMAND_UNIX_CURRENT = "mak./e";
  private static final String NON_COMPLIANT_COMMAND_UNIX_HOME = "bin~/make";
  private static final String NON_COMPLIANT_COMMAND_WINDOWS_ABSOLUTE = "7:\\\\make";
  private static final String NON_COMPLIANT_COMMAND_WINDOWS_PARENT = "m..\\ake";
  private static final String NON_COMPLIANT_COMMAND_WINDOWS_CURRENT = "ma.\\ke";
  private static final String NON_COMPLIANT_COMMAND_WINDOWS_NETWORK = "SERVER\\make";


  private static final String COMPLIANT_COMMAND_UNIX_ABSOLUTE = "/usr/bin/make";
  private static final String COMPLIANT_COMMAND_UNIX_PARENT = "../make";
  private static final String COMPLIANT_COMMAND_UNIX_CURRENT = "./make";
  private static final String COMPLIANT_COMMAND_UNIX_HOME = "~/bin/make";
  private static final String COMPLIANT_COMMAND_WINDOWS_ABSOLUTE = "Z:\\make";
  private static final String COMPLIANT_COMMAND_WINDOWS_PARENT = "..\\make";
  private static final String COMPLIANT_COMMAND_WINDOWS_CURRENT = ".\\make";
  private static final String COMPLIANT_COMMAND_WINDOWS_NETWORK = "\\\\SERVER\\make";

  private static final String[] NON_COMPLIANT_COMMAND_ARRAY = new String[]{"make"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_UNIX_PARENT = new String[]{"m../ake"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_UNIX_CURRENT = new String[]{"m./ake"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_UNIX_HOME = new String[]{"m~/ake"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_ABSOLUTE = new String[]{"7:\\\\make"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_PARENT = new String[]{"m..\\ake"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_CURRENT = new String[]{"m.\\ake"};
  private static final String[] NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_NETWORK = new String[]{"SERVER\\make"};

  private static final String[] COMPLIANT_COMMAND_ARRAY_EMPTY = {};
  private static final String[] COMPLIANT_COMMAND_ARRAY = new String[]{"/usr/bin/make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_UNIX_PARENT = new String[]{"../make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_UNIX_CURRENT = new String[]{"./make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_UNIX_HOME = new String[]{"~/bin/make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_WINDOWS_ABSOLUTE = new String[]{"W:\\\\make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_WINDOWS_PARENT = new String[]{"..\\bin\\make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_WINDOWS_CURRENT = new String[]{".\\make"};
  private static final String[] COMPLIANT_COMMAND_ARRAY_WINDOWS_NETWORK = new String[]{"\\\\SERVER\\make"};

  private static final String[] ENVIRONMENT = new String[]{"DEBUG=true"};
  private static final File FILE = null;

  private static final List<String> NON_COMPLIANT_COMMAND_LIST = Arrays.asList("make");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_UNIX_PARENT = Arrays.asList("m../ake");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_UNIX_CURRENT = Arrays.asList("mak./e");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_UNIX_HOME = Arrays.asList("bin~/make");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_WINDOWS_ABSOLUTE = Arrays.asList("7:\\\\make");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_WINDOWS_PARENT = Arrays.asList("m..\\ake");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_WINDOWS_CURRENT = Arrays.asList("ma.\\ke");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_WINDOWS_NETWORK = Arrays.asList("SERVER\\make");
  private static final List<String> NON_COMPLIANT_COMMAND_LIST_VARIABLE = Arrays.asList(NON_COMPLIANT_COMMAND);

  private static final List<String> COMPLIANT_COMMAND_LIST_UNIX = Arrays.asList("/usr/bin/make");
  private static final List<String> COMPLIANT_COMMAND_LIST_UNIX_PARENT = Arrays.asList("../make");
  private static final List<String> COMPLIANT_COMMAND_LIST_UNIX_CURRENT = Arrays.asList("./make");
  private static final List<String> COMPLIANT_COMMAND_LIST_UNIX_HOME = Arrays.asList("~/bin/make");
  private static final List<String> COMPLIANT_COMMAND_LIST_WINDOWS_ABSOLUTE = Arrays.asList("Z:\\make");
  private static final List<String> COMPLIANT_COMMAND_LIST_WINDOWS_PARENT = Arrays.asList("..\\make");
  private static final List<String> COMPLIANT_COMMAND_LIST_WINDOWS_CURRENT = Arrays.asList(".\\make");
  private static final List<String> COMPLIANT_COMMAND_LIST_WINDOWS_NETWORK = Arrays.asList("\\\\SERVER\\make");
  private static final List<String> COMPLIANT_COMMAND_LIST_VARIABLE = Arrays.asList(File.pathSeparator);

  private static final String NULL_INITIALIZED_COMMAND = null;
  private static final String[] NULL_INITIALIZED_COMMAND_ARRAY = null;
  private static final List<String> NULL_INITIALIZED_COMMAND_LIST = null;

  public void execString() throws IOException {
    Runtime.getRuntime().exec(("make"));  // Noncompliant [[sc=32;ec=38]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("make");  // Noncompliant [[sc=31;ec=37]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("usr/bin/make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("m./ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("m../ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("bin~/make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("7:\\\\../ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("SERVER\\make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("/usr/bin/make");
    Runtime.getRuntime().exec("/usr/bin/make -j8");
    Runtime.getRuntime().exec("../make");
    Runtime.getRuntime().exec("./make");
    Runtime.getRuntime().exec("~/bin/make");
    Runtime.getRuntime().exec("Z:\\make");
    Runtime.getRuntime().exec("..\\make");
    Runtime.getRuntime().exec(".\\make");
    Runtime.getRuntime().exec("\\\\SERVER\\make");

    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND);  // Noncompliant  [[sc=31;ec=52]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_UNIX_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_UNIX_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_UNIX_HOME);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_WINDOWS_ABSOLUTE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_WINDOWS_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_WINDOWS_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_WINDOWS_NETWORK);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_UNIX_ABSOLUTE);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_UNIX_PARENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_UNIX_CURRENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_UNIX_HOME);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_WINDOWS_ABSOLUTE);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_WINDOWS_PARENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_WINDOWS_CURRENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_WINDOWS_NETWORK);


    Runtime.getRuntime().exec("make", ENVIRONMENT);  // Noncompliant [[sc=31;ec=37]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("/usr/bin/make", ENVIRONMENT);

    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND, ENVIRONMENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_UNIX_ABSOLUTE, ENVIRONMENT);

    Runtime.getRuntime().exec("make", ENVIRONMENT, FILE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec("/usr/bin/make", ENVIRONMENT, FILE);

    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND, ENVIRONMENT, FILE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_UNIX_ABSOLUTE, ENVIRONMENT, FILE);
  }

  private void execArray() throws IOException {
    Runtime.getRuntime().exec((new String[]{"make"}));  // Noncompliant [[sc=32;ec=52]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{("make")});  // Noncompliant [[sc=31;ec=53]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"make"});  // Noncompliant [[sc=31;ec=51]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"usr/bin/make"});  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"m./ake"});  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"m../ake"});  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"bin~/make"});  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"7:\\\\../ake"});  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"SERVER\\make"});  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{});
    Runtime.getRuntime().exec(new String[]{"/usr/bin/make"});
    Runtime.getRuntime().exec(new String[]{"/usr/bin/make", "-j8"});
    Runtime.getRuntime().exec(new String[]{"../make"});
    Runtime.getRuntime().exec(new String[]{"./make"});
    Runtime.getRuntime().exec(new String[]{"~/bin/make"});
    Runtime.getRuntime().exec(new String[]{"Z:\\make"});
    Runtime.getRuntime().exec(new String[]{"..\\make"});
    Runtime.getRuntime().exec(new String[]{".\\make"});
    Runtime.getRuntime().exec(new String[]{"\\\\SERVER\\make"});
    Runtime.getRuntime().exec(new String[]{File.pathSeparator});


    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_UNIX_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_UNIX_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_UNIX_HOME);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_ABSOLUTE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_NETWORK);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_EMPTY);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_UNIX_PARENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_UNIX_CURRENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_UNIX_HOME);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_WINDOWS_ABSOLUTE);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_WINDOWS_PARENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_WINDOWS_CURRENT);
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY_WINDOWS_NETWORK);

    String[] nonCompliantCommandArray = new String[]{"make"};
    Runtime.getRuntime().exec(nonCompliantCommandArray); // Noncompliant [[sc=31;ec=55]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    String[] compliantCommandArray = new String[]{"/usr/bin/make"};
    Runtime.getRuntime().exec(compliantCommandArray);


    Runtime.getRuntime().exec(new String[]{"make"}, ENVIRONMENT);  // Noncompliant [[sc=31;ec=51]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"/usr/bin/make"}, ENVIRONMENT);

    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY, ENVIRONMENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY, ENVIRONMENT); // Compliant FN Cannot read from non-final variables

    Runtime.getRuntime().exec(nonCompliantCommandArray, ENVIRONMENT); // Noncompliant [[sc=31;ec=55]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(compliantCommandArray, ENVIRONMENT);

    Runtime.getRuntime().exec(new String[]{"make"}, ENVIRONMENT, FILE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(new String[]{"/usr/bin/make"}, ENVIRONMENT, FILE);

    Runtime.getRuntime().exec(NON_COMPLIANT_COMMAND_ARRAY, ENVIRONMENT, FILE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(COMPLIANT_COMMAND_ARRAY, ENVIRONMENT, FILE); // Compliant FN Cannot read from non-final variables

    Runtime.getRuntime().exec(nonCompliantCommandArray, ENVIRONMENT, FILE); // Noncompliant [[sc=31;ec=55]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    Runtime.getRuntime().exec(compliantCommandArray, ENVIRONMENT, FILE);
  }

  private void command() {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command();

    builder.command(("make"));  // Noncompliant [[sc=22;ec=28]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("make");  // Noncompliant [[sc=21;ec=27]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("usr/bin/make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("m./ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("m../ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("bin~/make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("7:\\\\../ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("SERVER\\make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command("/usr/bin/make");
    builder.command("/usr/bin/make -j8");
    builder.command("/usr/bin/make", "-j8");
    builder.command("../make");
    builder.command("./make");
    builder.command("~/bin/make");
    builder.command("Z:\\make");
    builder.command("..\\make");
    builder.command(".\\make");
    builder.command("\\\\SERVER\\make");

    builder.command(NON_COMPLIANT_COMMAND_ARRAY);  // Noncompliant [[sc=21;ec=48]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_UNIX_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_UNIX_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_UNIX_HOME);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_ABSOLUTE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_ARRAY_WINDOWS_NETWORK);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(COMPLIANT_COMMAND_ARRAY_EMPTY);
    builder.command(COMPLIANT_COMMAND_ARRAY);
    builder.command(COMPLIANT_COMMAND_ARRAY_UNIX_PARENT);
    builder.command(COMPLIANT_COMMAND_ARRAY_UNIX_CURRENT);
    builder.command(COMPLIANT_COMMAND_ARRAY_UNIX_HOME);
    builder.command(COMPLIANT_COMMAND_ARRAY_WINDOWS_ABSOLUTE);
    builder.command(COMPLIANT_COMMAND_ARRAY_WINDOWS_PARENT);
    builder.command(COMPLIANT_COMMAND_ARRAY_WINDOWS_CURRENT);
    builder.command(COMPLIANT_COMMAND_ARRAY_WINDOWS_NETWORK);
  }

  private void commandList() {
    ProcessBuilder builder = new ProcessBuilder();
    builder.command((Arrays.asList("make")));  // Noncompliant [[sc=22;ec=43]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList(("make")));  // Noncompliant [[sc=21;ec=44]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("make"));  // Noncompliant [[sc=21;ec=42]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("m../ake"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("mak./e"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("bin~/make"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("7:\\\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("m..\\ake"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("ma.\\ke"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("SERVER\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Arrays.asList("/usr/bin/make"));
    builder.command(Arrays.asList("/usr/bin/make -j8"));
    builder.command(Arrays.asList("/usr/bin/make", "-j8"));
    builder.command(Arrays.asList("../make"));
    builder.command(Arrays.asList("./make"));
    builder.command(Arrays.asList("~/bin/make"));
    builder.command(Arrays.asList("Z:\\make"));
    builder.command(Arrays.asList("..\\make"));
    builder.command(Arrays.asList(".\\make"));
    builder.command(Arrays.asList("\\\\SERVER\\make"));

    builder.command(NON_COMPLIANT_COMMAND_LIST);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_UNIX_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_UNIX_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_UNIX_HOME);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_WINDOWS_ABSOLUTE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_WINDOWS_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_WINDOWS_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_WINDOWS_NETWORK);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(NON_COMPLIANT_COMMAND_LIST_VARIABLE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(COMPLIANT_COMMAND_LIST_UNIX);
    builder.command(COMPLIANT_COMMAND_LIST_UNIX_PARENT);
    builder.command(COMPLIANT_COMMAND_LIST_UNIX_CURRENT);
    builder.command(COMPLIANT_COMMAND_LIST_UNIX_HOME);
    builder.command(COMPLIANT_COMMAND_LIST_WINDOWS_ABSOLUTE);
    builder.command(COMPLIANT_COMMAND_LIST_WINDOWS_PARENT);
    builder.command(COMPLIANT_COMMAND_LIST_WINDOWS_CURRENT);
    builder.command(COMPLIANT_COMMAND_LIST_WINDOWS_NETWORK);

    builder.command(Collections.singletonList("make"));  // Noncompliant [[sc=21;ec=54]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("m../ake"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("mak./e"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("bin~/make"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("7:\\\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("m..\\ake"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("ma.\\ke"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("SERVER\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(Collections.singletonList("/usr/bin/make"));
    builder.command(Collections.singletonList("/usr/bin/make -j8"));
    builder.command(Collections.singletonList("../make"));
    builder.command(Collections.singletonList("./make"));
    builder.command(Collections.singletonList("~/bin/make"));
    builder.command(Collections.singletonList("Z:\\make"));
    builder.command(Collections.singletonList("..\\make"));
    builder.command(Collections.singletonList(".\\make"));
    builder.command(Collections.singletonList("\\\\SERVER\\make"));
  }

  private void processBuilder() {
    new ProcessBuilder(("make"));  // Noncompliant [[sc=25;ec=31]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("make");  // Noncompliant [[sc=24;ec=30]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("m../ake");   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("mak./e");   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("bin~/make");   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("7:\\\\make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("m..\\ake");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("ma.\\ke");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("SERVER\\make");  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder("/usr/bin/make");
    new ProcessBuilder("/usr/bin/make -j8");
    new ProcessBuilder("/usr/bin/make", "-j8");
    new ProcessBuilder("../make");
    new ProcessBuilder("./make");
    new ProcessBuilder("~/bin/make");
    new ProcessBuilder("Z:\\make");
    new ProcessBuilder("..\\make");
    new ProcessBuilder(".\\make");
    new ProcessBuilder("\\\\SERVER\\make");

    new ProcessBuilder(NON_COMPLIANT_COMMAND);  // Noncompliant [[sc=24;ec=45]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_UNIX_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_UNIX_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_UNIX_HOME);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_WINDOWS_ABSOLUTE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_WINDOWS_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_WINDOWS_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_WINDOWS_NETWORK);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(COMPLIANT_COMMAND_UNIX_ABSOLUTE);
    new ProcessBuilder(COMPLIANT_COMMAND_UNIX_PARENT);
    new ProcessBuilder(COMPLIANT_COMMAND_UNIX_CURRENT);
    new ProcessBuilder(COMPLIANT_COMMAND_UNIX_HOME);
    new ProcessBuilder(COMPLIANT_COMMAND_WINDOWS_ABSOLUTE);
    new ProcessBuilder(COMPLIANT_COMMAND_WINDOWS_PARENT);
    new ProcessBuilder(COMPLIANT_COMMAND_WINDOWS_CURRENT);
    new ProcessBuilder(COMPLIANT_COMMAND_WINDOWS_NETWORK);
  }

  private void processBuilderList() {
    new ProcessBuilder((Arrays.asList("make")));  // Noncompliant [[sc=25;ec=46]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList(("make")));  // Noncompliant [[sc=24;ec=47]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("make"));  // Noncompliant [[sc=24;ec=45]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("m../ake"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("mak./e"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("bin~/make"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("7:\\\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("m..\\ake"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("ma.\\ke"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList("SERVER\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Arrays.asList());
    new ProcessBuilder(Arrays.asList("/usr/bin/make"));
    new ProcessBuilder(Arrays.asList("/usr/bin/make -j8"));
    new ProcessBuilder(Arrays.asList("/usr/bin/make", "-j8"));
    new ProcessBuilder(Arrays.asList("../make"));
    new ProcessBuilder(Arrays.asList("./make"));
    new ProcessBuilder(Arrays.asList("~/bin/make"));
    new ProcessBuilder(Arrays.asList("Z:\\make"));
    new ProcessBuilder(Arrays.asList("..\\make"));
    new ProcessBuilder(Arrays.asList(".\\make"));
    new ProcessBuilder(Arrays.asList("\\\\SERVER\\make"));

    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST);  // Noncompliant [[sc=24;ec=50]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_UNIX_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_UNIX_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_UNIX_HOME);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_WINDOWS_ABSOLUTE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_WINDOWS_PARENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_WINDOWS_CURRENT);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_WINDOWS_NETWORK);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(NON_COMPLIANT_COMMAND_LIST_VARIABLE);  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_UNIX);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_UNIX_PARENT);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_UNIX_CURRENT);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_UNIX_HOME);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_WINDOWS_ABSOLUTE);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_WINDOWS_PARENT);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_WINDOWS_CURRENT);
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_WINDOWS_NETWORK);

    new ProcessBuilder(Collections.singletonList("make"));  // Noncompliant [[sc=24;ec=57]] {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("m../ake"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("mak./e"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("bin~/make"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("7:\\\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("m..\\ake"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("ma.\\ke"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("SERVER\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(Collections.singletonList("/usr/bin/make"));
    new ProcessBuilder(Collections.singletonList("/usr/bin/make -j8"));
    new ProcessBuilder(Collections.singletonList("../make"));
    new ProcessBuilder(Collections.singletonList("./make"));
    new ProcessBuilder(Collections.singletonList("~/bin/make"));
    new ProcessBuilder(Collections.singletonList("Z:\\make"));
    new ProcessBuilder(Collections.singletonList("..\\make"));
    new ProcessBuilder(Collections.singletonList(".\\make"));
    new ProcessBuilder(Collections.singletonList("\\\\SERVER\\make"));
  }

  public void falseNegatives() throws IOException {
    String nonCompliantCommand = "make";
    Runtime.getRuntime().exec(nonCompliantCommand); // Compliant FN Cannot read from non-final strings
    String compliantCommand = "/usr/bin/make";
    Runtime.getRuntime().exec(compliantCommand);
    Runtime.getRuntime().exec(nonCompliantCommand, ENVIRONMENT); // Compliant FN Cannot read from non-final strings
    Runtime.getRuntime().exec(compliantCommand, ENVIRONMENT); // Compliant FN Cannot read from non-final strings
    Runtime.getRuntime().exec(nonCompliantCommand, ENVIRONMENT, FILE); // Compliant FN Cannot read from non-final strings
    Runtime.getRuntime().exec(compliantCommand, ENVIRONMENT, FILE); // Compliant FN Cannot read from non-final strings
    Runtime.getRuntime().exec(new String[]{System.lineSeparator()});  // Compliant FN Not resolving method calls

    String[] nonCompliantCommandArray = new String[]{"make"};
    nonCompliantCommandArray = new String[]{"make"};
    Runtime.getRuntime().exec(nonCompliantCommandArray); // Compliant FN Cannot read from non-final variables
    String[] compliantCommandArray = new String[]{"/usr/bin/make"};
    compliantCommandArray = new String[]{"/usr/bin/make"};
    Runtime.getRuntime().exec(compliantCommandArray); // Compliant FN Cannot read from non-final variablesRuntime.getRuntime().exec(nonCompliantCommandArray, ENVIRONMENT); // Compliant FN Cannot read from non-final variables
    Runtime.getRuntime().exec(compliantCommandArray, ENVIRONMENT); // Compliant FN Cannot read from non-final variables
    Runtime.getRuntime().exec(nonCompliantCommandArray, ENVIRONMENT, FILE); // Compliant FN Cannot read from non-final variables
    Runtime.getRuntime().exec(compliantCommandArray, ENVIRONMENT, FILE); // Compliant FN Cannot read from non-final variables

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(nonCompliantCommand); // Compliant FN Cannot read from non-final strings
    builder.command(compliantCommand);
    builder.command(COMPLIANT_COMMAND_LIST_VARIABLE);  // Compliant but we don't look into member select
    new ProcessBuilder(COMPLIANT_COMMAND_LIST_VARIABLE);  // Compliant but we don't look into member select
    new ProcessBuilder(Stream.of("make").collect(Collectors.toList()));

    Runtime.getRuntime().exec(NULL_INITIALIZED_COMMAND);
    Runtime.getRuntime().exec(NULL_INITIALIZED_COMMAND_ARRAY);
    new ProcessBuilder(NULL_INITIALIZED_COMMAND);
    new ProcessBuilder(NULL_INITIALIZED_COMMAND_LIST);
    new ProcessBuilder().command(NULL_INITIALIZED_COMMAND);
    new ProcessBuilder().command(NULL_INITIALIZED_COMMAND_ARRAY);
    new ProcessBuilder().command(NULL_INITIALIZED_COMMAND_LIST);
  }
}
