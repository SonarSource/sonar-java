import org.eclipse.core.runtime.Assert;

class EclipseAssert {
  void isLegal() {
    Object o = null;
    Assert.isLegal(o != null);
    o.toString();
  }
  void isLegal(String s) {
    Object o = null;
    Assert.isLegal(o != null, s);
    o.toString();
  }
  void isNotNull() {
    Object o = null;
    Assert.isNotNull(o);
    o.toString();
  }
  void isNotNull(String s) {
    Object o = null;
    Assert.isNotNull(o, s);
    o.toString();
  }
  void isTrue() {
    Object o = null;
    Assert.isTrue(o != null);
    o.toString();
  }
  void isTrue(String s) {
    Object o = new Object();
    Assert.isTrue(o != null, s);
    o.toString();
  }
  void isTrue2(String s) {
    Object o = null;
    Assert.isTrue(o != null, s);
    o.toString();
  }
}
