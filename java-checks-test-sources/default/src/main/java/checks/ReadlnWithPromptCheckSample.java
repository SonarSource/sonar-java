package checks;

public class ReadlnWithPromptCheckSample {

  void nonCompliant() {
    IO.print("Enter your name: ");
    String name = IO.readln(); // Noncompliant {{Use "IO.readln(prompt)" instead of separate "IO.print(prompt)" and "IO.readln()" calls.}}
//                ^^^^^^^^^^^
    IO.print(name);

    IO.println("Enter your age: ");
    String age = IO.readln(); // Noncompliant {{Use "IO.readln(prompt)" instead of separate "IO.println(prompt)" and "IO.readln()" calls.}}
//               ^^^^^^^^^^^
    IO.print(age);

    IO.print("Enter city: ");
    IO.readln(); // Noncompliant {{Use "IO.readln(prompt)" instead of separate "IO.print(prompt)" and "IO.readln()" calls.}}
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
