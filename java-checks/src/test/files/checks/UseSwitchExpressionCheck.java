
class A {

  int field;

  enum DoW {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
  }

  void foo(DoW day) {
    int numLetters;
    switch (day) {  // Noncompliant {{Use expression switch to set value of 'numLetters'.}}
      case MONDAY:
      case FRIDAY:
      case SUNDAY:
        numLetters = 6;
        break;
      case TUESDAY:
        numLetters = 7;
        break;
      case THURSDAY:
      case SATURDAY:
        numLetters = 8;
        break;
      case WEDNESDAY:
        numLetters = 9;
        break;
      default:
        throw new IllegalStateException("Wat: " + day);
    }
  }

  void two_throwing_branches(DoW day) {
    boolean isWeekday = false;
    switch (day) {  // Noncompliant {{Use expression switch to set value of 'isWeekday'.}}
      case MONDAY:
      case TUESDAY:
      case WEDNESDAY:
      case THURSDAY:
      case FRIDAY:
        isWeekday = true;
        break;
      case SUNDAY:
        throw new IllegalStateException();
      case SATURDAY:
        throw new IllegalStateException();
      default:
        throw new IllegalStateException();
    }
  }

  void set_field(int x) {
    switch (x) { // Noncompliant {{Use expression switch to set value of 'field'.}}
      case 1:
        this.field = 1;
        break;
      case 2:
        field = 2;
        break;
      default:
        throw new IllegalStateException();
    }
  }

  int return_switch(int x) {
    switch (x) { // Noncompliant {{Use expression switch to return the value.}}
      case 1:
        return 1;
      case 2:
        return 2;
      default:
        throw new IllegalStateException();
    }
  }

  void fallthrough(int a) {
    int b;
    switch (a) {
      case 1:
        b = 3;
        System.out.println("falling");
      case 2:
        b = 4;
        break;
      default:
        b = 1;
        break;
    }
  }

  void complex_stmt(int a) {
    int b;
    switch (a) {
      case 1:
        if (foo) {
          b = 1;
        }
        break;
      case 2:
        b = 4;
        break;
      default:
        b = 1;
        break;
    }
  }

  void not_simple_assignment(int a) {
    int b = 0;
    switch (a) {
      case 1:
        b += 1;
        break;
      case 2:
        b = 4;
        break;
      default:
        b = 1;
        break;
    }
  }

  void another_var(int a) {
    int b;
    int c;
    switch (a) {
      case 1:
        b = 3;
        break;
      case 2:
        c = 4;
        break;
      default:
        b = 1;
        break;
    }
  }

  void sideeffects(int a) {
    int b;
    switch (a) {
      case 1:
        b = 3;
        System.out.println("hello");
        break;
      case 2:
        b = 4;
        break;
      default:
        b = 1;
        break;
    }
  }

  int sideeffects2(int a) {
    int b;
    switch (a) {
      case 1:
        return 3;
      case 2:
        return 4;
      default:
        b = 1;
        break;
    }
    return 1;
  }

  void set_array(int x) {
    int[] arr;
    switch (x) {
      case 1:
        arr[1] = 1;
        break;
      case 2:
        arr[2] = 2;
        break;
    }
  }

}
