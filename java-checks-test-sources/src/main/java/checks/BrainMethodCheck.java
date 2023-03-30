package checks;

class BrainMethodCheck {

  void foo() { // Noncompliant [[sl=0;sc=3;el=+65;ec=4]] {{Refactor this brain method to reduce its complexity.}}
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 1: {
            System.out.println("case 1");
          }
          default:
            return;
        }
      }
    } else {
      for (String s : new String[] {"a"}) {
        System.out.println(s);
      }
    }
    System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 2: {
            System.out.println("case 2");
          }
          default: return;
        }
      }
    } else {
      for (String s : new String[] {"a", "b"}) {
        System.out.println(s);
      }
    }
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 3: {
            System.out.println("case 3");
          }
          default:
            return;
        }
      }
    } else {
      for (String s : new String[] {"a", "b", "c"}) {
        System.out.println(s);
      }
    }
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 4: {
            System.out.println("case 4");
          }
          default:
            return;
        }
      }
    } else {
      for (String s : new String[] {"a", "b", "c", "d"}) {
        System.out.println(s);
      }
    }
  }

  void boo() { // Compliant: does not break LOC threshold
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 1: {
            System.out.println("case 1");
          }
          default: return;
        }
      }
    }
    System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 2: {
            System.out.println("case 2");
          }
          default: return;
        }
      }
    } else {
      for (String s : new String[] {"a", "b"}) {
        System.out.println(s);
      }
    }
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 3: {
            System.out.println("case 3");
          }
          default: return;
        }
      }
    } else {
      for (String s : new String[] {"a", "b", "c"}) {
        System.out.println(s);
      }
    }
    if (a != b) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 4: {
            System.out.println("case 4");
          }
          default: return;
        }
      }
    } else {
      for (String s : new String[] {"a", "b", "c", "d"}) {
        System.out.println(s);
      }
    }
  }
  
  void doo() { // Compliant: does not break cyclomatic complexity threshold
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    for(int num : new int[] {1,2,3}) {
      for (int i = 0; i < x; i++) {
        switch (x) {
          case 1: {
            System.out.println("case 1");
          }
          case 2: {
            System.out.println("case 2");
          }
          case 3: {
            System.out.println("case 3");
          }
          case 4: {
            System.out.println("case 4");
          }
          default:
            return;
        }
      }
    } 
    for (String s : new String[] {"a"}) {
      System.out.println(s);
    }
    int z = x + y;
    int side1 = z;
    int side2 = z + (z/2);
    int area = side1 * side2;
    int height = 12;
    int volume = area * height;
    int[] pointA = new int[] {2, 5, 4};
    int[] pointB = new int[] {2, 3, 4};
    int[] pointC = new int[] {4, 3, 7};
    class Triangle{
      Vector3 pointA;
      Vector3 pointB;
      Vector3 pointC;
      public Triangle(int[] v1, int[] v2, int[] v3) {
        this.pointA = new Vector3(v1);
        this.pointB = new Vector3(v2);
        this.pointC = new Vector3(v3);
      }
      class Vector3{
        int x;
        int y;
        int z;
        public Vector3(int[] point) {
          this.x = point[0];
          this.y = point[1];
          this.z = point[2];
        }
      }
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(pointA.toString());
        sb.append("\n");
        sb.append(pointB.toString());
        sb.append("\n");
        sb.append(pointC.toString());
        sb.append("\n");
        return sb.toString();
      }
    }
    Triangle triangle = new Triangle(pointA, pointB, pointC);
  }
  
  void zoo() { // Compliant: does not break number of accessed variables threshold
    int x = 0;
    int y = 1;
    for (int i = 0; i < x; i++) {
      if (x != y) {
        switch (x) {
          case 1: {
            System.out.println("case 1");
          }
          default:
            return;
        }
      } else {
        if(x < -10){
          System.out.println("try");
        }else{
          if(y < -10) {
            System.out.println("catch");
          }
        }
      }
      System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");
      if (x != y) {
        switch (x) {
          case 2: {
            System.out.println("case 2");
          }
          default:
            return;
        }
      } else {
        if(x < -10){
          System.out.println("try");
        }else{
          if(y < -10) {
            System.out.println("catch");
          }
        }
      }
      if (x != y) {
        switch (x) {
          case 3: {
            System.out.println("case 3");
          }
          default:
            return;
        }
      } else {
        if(x < -10){
          System.out.println("try");
        }else{
          if(y < -10) {
            System.out.println("catch");
          }
        }
      }
      if (x != y) {
        switch (x) {
          case 4: {
            System.out.println("case 4");
          }
          case 5:{
            if(x < 0) {
              System.out.println("case 5.1");
            }else {
              System.out.println("case 5");
            }
          }
          default:
            return;
        }
      } else {
        try{
          System.out.println("try");
        }catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    }

  }
  
  void moo() { // Compliant: does not break nesting level threshold
    String a = "a";
    String b = "b";
    int x = 0;
    int y = 1;
    if (a != b) {
      for (int i = 0; i < x; i++) {
        System.out.println("case for");
      }
      switch (x) {
        case 1: {
          System.out.println("case 1");
        }
        default:
          return;
      }
    } else {
      for (String s : new String[] {"a"}) {
        System.out.println(s);
      }
    }
    System.out.println("This method is too long, has too many variables, it is too nested, and it's too complex");
    if (a != b) {
      for (int i = 0; i < x; i++) {
        System.out.println("case for 2");
      }
      switch (x) {
        case 2: {
          System.out.println("case 2");
        }
        default: return;
      }
    } else {
      for (String s : new String[] {"a", "b"}) {
        System.out.println(s);
      }
    }
    if (a != b) {
      for (int i = 0; i < x; i++) {
        System.out.println("case for 3");
      }
      switch (x) {
        case 3: {
          System.out.println("case 3");
        }
        default:
          return;
      }
    } else {
      for (String s : new String[] {"a", "b", "c"}) {
        System.out.println(s);
      }
    }
    if (a != b) {
      for (int i = 0; i < x; i++) {
        System.out.println("case for 4");
      }
      switch (x) {
        case 4: {
          System.out.println("case 4");
        }
        default:
          return;
      }
    } else {
      for (String s : new String[] {"a", "b", "c", "d"}) {
        System.out.println(s);
      }
    }
  }
  
}
