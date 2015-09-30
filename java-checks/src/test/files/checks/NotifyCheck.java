class MyThread extends Thread{

  @Override
  public void run(){
    synchronized(this){
      // ...
      notify();  // Noncompliant {{"notify" may not wake up the appropriate thread.}}
      notifyAll();
    }
  }
}
