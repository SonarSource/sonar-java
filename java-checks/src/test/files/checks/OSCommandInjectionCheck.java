import java.util.ArrayList;
import java.util.List;

class A {
  public void listContent(String input) {
    Runtime rt = Runtime.getRuntime();
    String[] cmds = new String[]{"" , input};
    // input could easily contain extra commands
    rt.exec("ls " + input); // Noncompliant {{Make sure "input" is properly sanitized before use in this OS command.}}
    rt.exec(cmds); // Noncompliant
    rt.exec(new String[]{" ", input}); // Noncompliant {{Make sure "input" is properly sanitized before use in this OS command.}}
    rt.exec(new String[]{" ", " "}); // Compliant
  }

  public void execute(String command, String argument) {
    
    ProcessBuilder pb = new ProcessBuilder(
      command, // Noncompliant {{Make sure "command" is properly sanitized before use in this OS command.}}
      argument); // Noncompliant [[sc=7;ec=15]] {{Make sure "argument" is properly sanitized before use in this OS command.}}

    pb = new ProcessBuilder(
      command, // Noncompliant
      argument); // Noncompliant
    
    pb = new ProcessBuilder("", argument); // Noncompliant
    
    pb.command(argument); // Noncompliant
    
    pb.command(command, // Noncompliant
               argument); // Noncompliant

    pb.command(" ",
      command, // Noncompliant
      argument); // Noncompliant

    String[] args = {"echo", command, argument};
    pb.command(args); // Noncompliant
    
    pb = new ProcessBuilder(getCommands());
    
    String[] args2 = new String[] {"echo", "alpha", "tango"};
    pb.command(args2); // compliant
    pb.command(new String[] {"echo", "alpha", "tango"}); // Compliant
    pb.command("echo", "alpha", "tango"); // Compliant
    pb.command(); // Compliant
    pb.command(getCommands()); // Compliant
  }
  
  private static List<String> getCommands() {
    return new ArrayList<String>();
  }


  class Commands {
    public static final String LS = "ls";
    public static final String ROOT = "/";
  }

  public static void main(String... args) throws Exception {
    ProcessBuilder b = new ProcessBuilder(Commands.LS, Commands.ROOT); //
    Process p = b.start();
    new BufferedReader(new InputStreamReader(p.getInputStream())).lines().forEach(System.out::println);
  }

  public void doStuff() throws IOException
  {
    String[] args = new String[] {
        "arg1", "arg2", "arg3"
    };
    ProcessBuilder builder = new ProcessBuilder(args);
    builder.start();
  }

}
