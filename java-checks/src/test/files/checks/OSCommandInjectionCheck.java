import java.util.ArrayList;
import java.util.List;

class A {
  public void listContent(String input) {
    Runtime rt = Runtime.getRuntime();
    String[] cmds;
    // input could easily contain extra commands
    rt.exec("ls " + input); // Noncompliant {{Make sure "input" is properly sanitized before use in this OS command.}}
    rt.exec(cmds); // Noncompliant
    rt.exec(new String[]{" ", input}); // Noncompliant {{Make sure "input" is properly sanitized before use in this OS command.}}
    rt.exec(new String[]{" ", " "}); // Compliant
  }

  public void execute(String command, String argument) {
    
    ProcessBuilder pb = new ProcessBuilder(
      command, // Noncompliant {{Make sure "command" is properly sanitized before use in this OS command.}}
      argument); // Noncompliant {{Make sure "argument" is properly sanitized before use in this OS command.}}

    pb = new ProcessBuilder(
      command, // Noncompliant
      argument); // Noncompliant
    
    pb = new ProcessBuilder("", argument); // Noncompliant
    
    pb = new ProcessBuilder(new String[]{" ", command}, // Noncompliant
                            argument); // Noncompliant
    
    pb.command(argument); // Noncompliant
    
    pb.command(command, // Noncompliant
               argument); // Noncompliant

    pb.command(" ",
      command, // Noncompliant
      argument); // Noncompliant

    String[] args = {"echo", command, argument};
    pb.command(args); // Noncompliant
    
    ProcessBuilder pb = new ProcessBuilder(getCommands());
    
    String[] args2 = new String[] {"echo", "alpha", "tango"};
    pb.command(args2); // Noncompliant
    pb.command(new String[] {"echo", "alpha", "tango"}); // Compliant
    pb.command("echo", "alpha", "tango"); // Compliant
    pb.command(); // Compliant
    pb.command(getCommands()); // Compliant
  }
  
  private static List<String> getCommands() {
    return new ArrayList<String>();
  }
}
