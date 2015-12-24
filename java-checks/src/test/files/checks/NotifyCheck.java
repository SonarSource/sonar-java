class MyThread extends Thread{

  @Override
  public void run(){
    synchronized(this){
      // ...
      notify();  // Noncompliant [[sc=7;ec=13]] {{"notify" may not wake up the appropriate thread.}}
      notifyAll();
    }
  }
}
