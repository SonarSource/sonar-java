                        class A {
                          void f() {
                            switch (x) {
/* compliant 5 lines */       case 0:
                                System.out.println(); // 1
                                System.out.println(); // 2
                                System.out.println(); // 3
                                System.out.println(); // 4
                                System.out.println(); // 5
/* Noncompliant - 7 lines */  case 1:
                                System.out.println(); // 1
                                System.out.println(); // 2
                                System.out.println(); // 3
                                System.out.println(); // 4
                                System.out.println(); // 5
                                System.out.println(); // 6
                                break;                // 7
/* Noncompliant - 6 lines */  case 2: { System.out.println();   // 1
                                System.out.println();           // 2
                                System.out.println();           // 3
                                System.out.println();           // 4
                                System.out.println();           // 5
                                System.out.println(); }         // 6
/* Noncompliant - 6 lines */  case 3:
                                System.out.println();           // 1
                                /* foo */                       // 2
                            
                                System.out.println(             // 4
                                );                              // 5
                                /* tata */                      // 6
                              case 4: // 1
/* Noncompliant - 6 lines */  case 5: // 1
                                      // 2
                                      // 3
                                      // 4
                                      // 5
                                      // 6
/* Noncompliant - 6 lines */  case 6:





/* Noncompliant - 6 lines */  case 7:
                                // my empty comment     // 1
                                System.out.println();   // 2
                                System.out.println();   // 3
                                System.out.println();   // 4
                                System.out.println();   // 5
                                System.out.println();   // 6
/* Noncompliant - 6 lines */  default:
                                System.out.println("");  // 1 
                                System.out.println("");  // 2
                                System.out.println("");  // 3
                                System.out.println("");  // 4
                                System.out.println("");  // 5
                                break;                   // 6
                            }

                            switch (myVariable) {
/* Compliant - 4 lines  */    case 0:
                                System.out.println("");  // 1
                                System.out.println("");  // 2
                                System.out.println("");  // 3
                                break;                   // 4
/* Compliant - 5 lines */    case 1:




                            }

                            switch (myVariable) {
                              case 0: System.out.println(); default: System.out.println(); }

                            switch (myVariable) {
                              case 0: // foo
                                System.out.println();
                                System.out.println();
                                System.out.println();
                                System.out.println();
                                break;
                              case 1:
                                System.out.println();
                            }

                          }
                        }
