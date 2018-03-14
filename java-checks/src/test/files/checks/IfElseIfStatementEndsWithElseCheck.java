class A{
  private static final boolean FALSE = false;
  private int x = 2;

  void foo() {
    if (x == 0) {       // Compliant
    } else if (x == 1) {
        if(x == 2) { 
        }
        else if(x == 3) { // Noncompliant
          if (x == 5) {  // Compliant
          }else {
          }
        }
    } else {
    }
    if(x == 0) {  
    }else if( x == 1) { // Noncompliant [[sc=6;ec=13]] {{"if ... else if" constructs should end with "else" clauses.}}
    }

    if(x==0) {  // Compliant
    }
    for(;;){
      if(x == 10) {  // Compliant
      }
      if(x == 11) {  
      }else if(x == 12){// Noncompliant  [[sc=8;ec=15]] {{"if ... else if" constructs should end with "else" clauses.}}
      }
    }
    if(x == 0) {} // Compliant
    else {}
    if(x == 13) { //Compliant 
    } else {
      if(x == 14) {  
      }else if(x == 15) {// Noncompliant  [[sc=8;ec=15]] {{"if ... else if" constructs should end with "else" clauses.}}
      }
    }
    if(x ==16) {
    }else if(x == 17) {
    }else if(x == 18) {  // Noncompliant
    }
    if(x ==16) {
    }else if(x == 17) {
    }else if(x == 18) {  // Compliant
    }else {}
    if(x ==16) {
    }else if(x == 17) {
    }else if(x == 18) {
    }else if(x == 19){}  // Noncompliant
  }

  void foo2(int op) {
    if (x == 20) {
    } else {             // Compliant
        if (x == 21) {
        }  
    }
  }
}
