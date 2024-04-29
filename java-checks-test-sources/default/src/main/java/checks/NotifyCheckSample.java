package checks;

class MyThread extends Thread{

  @Override
  public void run(){
    synchronized(this){
      // ...
      notify(); // Noncompliant [[quickfixes=qf1]] {{"notify" may not wake up the appropriate thread.}}
//    ^^^^^^
      // fix@qf1 {{Replace with "notifyAll()"}}
      // edit@qf1 [[sc=7;ec=13]] {{notifyAll}}
      notifyAll();
    }
  }
}
