
public class A {
  void nonCompliant() {
    if (condition)
      firstActionInBlock();
      secondAction();  // Noncompliant {{Only the first line of this 2-line block will be executed conditionally. The rest will execute unconditionally.}}
    thirdAction();

    if (condition) {
    } else if (condition) {
    } else
      firstActionInBlock();

      secondAction();  // Noncompliant {{Only the first line of this 3-line block will be executed conditionally. The rest will execute unconditionally.}}

    if (condition)
      action();
    else
      firstActionInBlock();
      secondAction();  // Noncompliant {{Only the first line of this 2-line block will be executed conditionally. The rest will execute unconditionally.}}

    String str = null;
    for (int i = 0; i < array.length; i++)
      str = array[i];
      doTheThing(str);  // Noncompliant {{Only the first line of this 2-line block will be executed in a loop. The rest will execute only once.}}

    while (true)
      firstActionInBlock();
      secondAction();  // Noncompliant {{Only the first line of this 2-line block will be executed in a loop. The rest will execute only once.}}

    int[] test = new int[]{1, 2};
    for (int intValue : test)
      firstActionInBlock();
      // comment
      // bla bla bla
      secondAction();  // Noncompliant {{Only the first line of this 4-line block will be executed in a loop. The rest will execute only once.}}
  }

  void compliant() {
    if (condition)
      action();
    outerAction;

    if (condition) {
      firstActionInBlock();
      secondAction();
    } else if (condition) {
    } else {
      other();
    }
    thirdAction();

    String str = null;
    for (int i = 0; i < array.length; i++) {
      str = array[i];
      doTheThing(str);
    }
  }
}
