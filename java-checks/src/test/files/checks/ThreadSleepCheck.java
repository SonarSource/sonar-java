class A {
  Object monitor;
  void foo() {
    Thread.sleep(12);
    Thread.sleep(12, 13);
    synchronized(monitor) {
      while(notReady()){
        Thread.sleep(200);    //NonCompliant
        Thread.sleep(200, 12);//NonCompliant
      }
      process();
    }
  }

  synchronized void foo() {
    Thread.sleep(200);    //NonCompliant
    Thread.sleep(200, 12);//NonCompliant
  }


}