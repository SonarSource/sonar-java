class UnusedMethodParameterCheck {

  public void unknownAnnotatedParameter(@Unknown Object event, int arg2) { // Compliant, unknown annotation on parameter
    System.out.println(arg2);
  }

}
