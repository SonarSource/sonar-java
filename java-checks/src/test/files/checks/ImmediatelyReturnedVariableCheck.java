class A{

  public long computeDurationInMilliseconds() {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
    return duration;
  }

  public void doSomething() {
    RuntimeException myException = new RuntimeException();
    throw myException;
  }

  public long computeDurationInMilliseconds() {
    return (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
  }

  public void doSomething() {
    throw new RuntimeException();
  }

  public long computeDurationInMilliseconds() {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
    duration = duration - 12;
    return duration;
  }

  public void doSomething() {
    RuntimeException myException = new RuntimeException();
    System.out.println(myException.getMessage());
    throw myException;
  }

  private String toto;
  public String getToto(){
    return toto;
  }

}
