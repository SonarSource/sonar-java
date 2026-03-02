package checks;

public class ReadlnWithPromptCheckSample {

  private static final String text = IO.readln("Enter your name: "); // Compliant

  void nonCompliant() {
    IO.print("Enter your name: ");
    String name = IO.readln(); // Noncompliant {{Use "IO.readln(String prompt)" instead of separate "IO.print(Object obj)" and "IO.readln()" calls.}}
//                ^^^^^^^^^^^
    IO.print(name);

    IO.println("Enter your age: ");
    String age = IO.readln(); // Noncompliant {{Use "IO.readln(String prompt)" instead of separate "IO.println(Object obj)" and "IO.readln()" calls.}}
//               ^^^^^^^^^^^
    IO.print(age);

    IO.print("Enter city: ");
    IO.readln(); // Noncompliant {{Use "IO.readln(String prompt)" instead of separate "IO.print(Object obj)" and "IO.readln()" calls.}}
//  ^^^^^^^^^^^
  }

  void compliant() {
    IO.readln(); // Compliant

    String name = IO.readln("Enter your name: "); // Compliant

    IO.println("Welcome!");
    String input = IO.readln("Please state your name:"); // Compliant

    IO.println();
    IO.readln(); // Compliant

    IO.print("Enter email: ");
    int x = 5;
    String email = IO.readln(); // Compliant
  }
}
