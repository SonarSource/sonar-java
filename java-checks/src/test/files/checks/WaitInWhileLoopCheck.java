import java.lang.Object;
import java.util.concurrent.locks.Condition;

class A {
  void foo() {
    Object obj;
    Condition condition;
    synchronized (obj) {
      if (!suitableCondition()){
        obj.wait(12);//NonCompliant
        condition.await();//NonCompliant
      }
      for(;;){
        obj.wait(12);//Compliant
      }
      do{
        obj.wait(12);//Compliant
      }while (!suitableCondition);


      while(!suitableCondition()){
        obj.wait(12);//Compliant
        condition.await();//Compliant
        while(!suitableCondition()) {
          obj.wait(12);//Compliant
          condition.await();//Compliant
        }
        obj.wait(12);//Compliant
      }

    }

  }

}