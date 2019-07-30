import java.io.IOException;

/**
 * Some class documentation
 *
 * @param <B> description
 * @param <C> FIXME
 * @param <D> description non-existing param
 */
class A<B, C, E> {
  /**
   * Main description
   * On serveral lines
   *
   * @param      a     param 1
   * @param      b param 2 with some more desc
   * @param      c
   * @param      d multilines parameter
   *               description
   * @return     return description
   * @exception  NullPointerException 1st exception description
   * @throws     IndexOutOfBoundsException second exception
   * description
   * @exception  IOException
   */
  private int foo(int a, int c, int d, int e) throws java.lang.NullPointerException, IOException, IllegalStateException, B {
    return 0;
  }

  /**
   * @return
   * return description starts on next line
   * @return 2nd return description
   */
  private int bar(int a) {
    return 0;
  }

  /**/
  private int emptyJavadoc1(int a) {
    return 0;
  }

  /***/
  private int emptyJavadoc2(int a) {
    return 0;
  }

  /*
   */
  private int emptyJavadoc3(int a) {
    return 0;
  }

  /**
   */
  private int emptyJavadoc4(int a) {
    return 0;
  }

  //
  private int emptyJavadoc5(int a) {
    return 0;
  }

  //*/
  private int emptyJavadoc6(int a) {
    return 0;
  }

  /**
   *
   * FIXME
   * @param a .
   * @param b
   * @param c ...
   * @param d TODO
   * @param e FIXME
   */
  private int emptyDescription(int a, int b, int c, int d, int e) throws java.lang.NullPointerException {
    return 0;
  }

  /**
   *
   * ...
   *
   * @param a a
   * @param b b
   * @param c c
   * @param d d
   * @param e e
   * @throws java.lang.NullPointerException exception with fully qualified name
   */
  private int fullParamsDescription(int a, int b, int c, int d, int e) throws NullPointerException {
    return 0;
  }

  /**
   *
   * @deprecated
   *
   * @throws IOException description is set
   * @throws java.io.ObjectStreamException TODO
   * @throws InvalidObjectException ...
   */
  private int genericExceptionThrown() throws Exception {
    return 0;
  }

  private int genericExceptionThrownUndocumented() throws Exception {
    return 0;
  }

  private int invalidThrownExceptionUndocumented() throws Exception<String> {
    return 0;
  }
}

/**
 * TODO
 */
class NoDocClass {

}
