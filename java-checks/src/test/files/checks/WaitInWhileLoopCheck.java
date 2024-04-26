import java.lang.Object;
import java.util.concurrent.locks.Condition;

class A {
  void foo() {
    Object obj;
    Condition condition;
    synchronized (obj) {
      if (!suitableCondition()){
        obj.wait(12); // Noncompliant {{Remove this call to "wait" or move it into a "while" loop.}}
//          ^^^^
        condition.await(); // Noncompliant
      }
      for(;;){
        obj.wait(12);
      }
      do{
        obj.wait(12);
      }while (!suitableCondition);


      while(!suitableCondition()){
        obj.wait(12);
        condition.await();
        while(!suitableCondition()) {
          obj.wait(12);
          condition.await();
        }
        obj.wait(12);
      }

    }

  }

}
