import java.util.List;

class A{
  void foo() {
    int myVar = 0;
    switch (myVar) {  // Compliant
      case 0:
        switch (0) {  // Noncompliant
          case 0:
          case 1:
            System.out.println();
            switch (1){ // Noncompliant [[sc=13;ec=19;el=+0]] {{Refactor the code to eliminate this nested "switch".}}
              case 0:
              case 1:
                break;
            }
          case 2:
            break;
        }
      case 1:
        switch (2) { // Noncompliant
          case 0:
          case 1: 
            switch (3) { // Noncompliant {{Refactor the code to eliminate this nested "switch".}}
              case 0: 
                System.out.println();
              case 1:
                break;
            }
            break;
          case 2: 
            break;
        }
        break;
      case 2: 
        break;
      default:{
        switch(4) {  // Noncompliant
          case 0:
            switch(5) { // Noncompliant
              default:
                break;
            } 
            break;
          case 1: {
            switch (6) {  // Noncompliant
              case 0:
                break;
            }
          }
        }
      }
    }
  }
}

class B{
  List<Integer> list;
  void foo2() {
    int i = 0;
    switch(i) {  // Compliant
      case 1:
        break;
      case 2: {
        for(;i<3;i++) {
          switch(i) { // Noncompliant
            case 0:
              break;
            case 2:{
              new Thread() {
                public void run() {
                  System.out.println();
                  int i = 0;
                  switch(i) {  // Compliant
                    case 0: 
                      break;
                    case 1:
                      break;
                  }
                }
              }.start();
              break;
            }
            case 3:
              list.stream().filter(x -> {
                switch(x) {  // Compliant 
                  case 0:
                    return true;
                  case 1:
                    return false;
                }
              });
              break;
            default:
              break;
          }
        }
      }
    }
  }
  void foo3(int i) {
    switch (i) {
      case 0:
      case 42:
        class B { // nested class
          void bar(int j) {
            switch (j) { // Compliant
              case 42:
                System.out.println("");
                break;
              case 0:
                break;
              default:
                break;
            }
          }
        }
        new B().bar(i);
        break;
      default:
        break;
    }
  }
}
