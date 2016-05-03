public class Person {
  String name;
  int age;
  String address;
  String address2;
  String address3;
  String address4;

  public synchronized void setName(String name) {
    this.name = name;
  }

  public String getName() {  // Noncompliant
    return this.name;
  }

  public void setAge(int age) {  // Noncompliant
    this.age = age;
  }

  public int getAge() {
    synchronized (this) {
      return this.age;
    }
  }

  public synchronized String getAddress() {
    return address;
  }

  public void setAddress(String address) { // Noncompliant {{Synchronize this method to match the synchronization on "getAddress".}}
    this.age = age;
  }

  public String getAddress2() {
    return address;
  }
  public abstract void setAddress2(String address);

  public String getAddress3() {
    synchronized (new Integer(2)) {
      return address3;
    }
  }
  public void setAddress3(String address) {
    this.address3 = address;
  }

  public void getNo() {}
  public Object setNo(int a) { return null;}
  public void setNo() {}

  public String getAddress4() {
    address4+="";
    synchronized (this) {
      return address4;
    }
  }
  public void setAddress4(String address) {
    this.address4 = address;
  }
}

public class Person2 {
  String name;
  int age;
  boolean old;
  boolean young;

  public synchronized void setName(String name) {
    this.name = name;
  }

  public synchronized String getName() {
    return this.name;
  }

  public void setAge(int age) {
    synchronized (this) {
      this.age = age;
    }
  }
  public int getAge() {
    synchronized (this) {
      return this.age;
    }
  }

  public synchronized boolean isOld() {
    return old;
  }

  public void setOld(boolean old) { // Noncompliant
    this.old = old;
  }

  public boolean isYoung() { // Noncompliant
    return young;
  }

  public synchronized void setYoung(boolean young) {
    this.young = young;
  }
}