class A {

  private void fun() {
    boolean b1 = false;
    if(object1==null || object2.getI() == object1.getI()) {
      b1 = true;
    }

    boolean b2 = (object1!=null && object1.getP().equals(object2.getP()) == false);

    if((b2 == false) && (b1 == true))
    {
      return;
    }
  }
}
