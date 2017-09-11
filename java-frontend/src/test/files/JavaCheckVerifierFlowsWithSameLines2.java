class A {

   void same_lines_both_wrong_messages() {
    b.toString(); // Noncompliant [[flows=wrong_msg1,wrong_msg2]] {{error}}  flow@wrong_msg1,wrong_msg2  {{wrong}}
    Object a = null; // flow@wrong_msg1 {{wrong}} flow@wrong_msg2 {{wrong}}
    Object b = new Object();  // flow@wrong_msg1,wrong_msg2 {{wrong}}
  }
}
