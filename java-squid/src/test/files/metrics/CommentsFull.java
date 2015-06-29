/* comment */
package example.foo;

/* comment */
import example.foo.I3;

import javax.lang.model.type.UnknownTypeException;

import java.util.Arrays;
import /* comment */ java.util.List;
import java.util.Map; // comment

import /* comment */ static org.springframework.util.StringUtils.arrayToDelimitedString;

import java.util. /* comment */ *;
import java.io.*;
import java.lang /* comment */ .*;
import java.lang.annotation.*;

/** comment */
interface I {}
interface I1 {}
interface I2 {}
interface I3 { int op(int a, int b); }

/** comment*/
enum E1 {}
enum E2 {
   /* comment */ A, 
   B /* comment */, 
   C /* comment */;
}
enum E3 {
  A /* comment */ (
    /* comment */),
  B (/* comment */ 0) 
  /* comment */{ 
    /* comment */};
  
  int i;
  E3() {}
  E3(int i) { this.i = i; }
}

@Target(/* comment */{
  /*comment */ ElementType.FIELD
  /* comment separators of arguments */, ElementType.ANNOTATION_TYPE,
  ElementType.TYPE_PARAMETER /* comment separators of arguments are ignored */,
  ElementType. /* comment */METHOD,
  ElementType.TYPE_USE /* comment*/}
/* comment */)
@interface A01 {}

/** comment */
public /* comment */ class CommentsFull {}
class /* comment */ A02 {}
class A03 /* comment */ {}
/* comment */ @Deprecated class A04 {}
class A05 /* comment */ <T> {}
class A06 </* comment */ T> {}
class A07 <T /* comment */> {}
class A08 </* comment */ @A01 T> {}
class A09 <T /* comment */, G> {}
class A10 <T /* comment */ extends I> {}
class A11 <T extends /* comment */ I> {}
class A12 <T extends /* comment */ @A01 I> {}
class A13 <T extends I1 /* comment separators of bound*/ & I2> {}
class A14 <T extends I1 & /* comment */ I2> {}
class A15 /* comment */ implements I {}
class A16 implements /* comment */ I {}
class A17 implements I1 /* comment separators of interface*/, I2 {}
class A18 implements I1, /* comment */ I2 {}
class A19 /* comment */ extends A02 {}

abstract class Rest<T> {
  /** comment */
  List /* comment */ a01;
  List a02 /* comment */;
  List a03; /* comment */
  List/* comment */<Integer> a04;
  List</* comment */Integer> a05;
  List<Integer /* comment */> a06;
  Map<Integer /* comment */, Integer> a07;
  List</* comment */ ?> a8;
  List</* comment */ ? extends I> a9;
  List<? /* comment */ extends I> a10;
  List<? extends /* comment */ I> a11;
  List<? /* comment */ super  A01> a12;
  List a13 /* comment */ = null;
  List a14 = /* comment */ null;
  List a15 =  /* comment */ new ArrayList();
  List a16 =  new /* comment */ ArrayList();
  List a17 =  new ArrayList /* comment */<>();
  List a18 =  new ArrayList </* comment */>();
  List a19 =  new ArrayList <>/* comment */();
  List a20 =  new ArrayList <>(/* comment */);
  List a21 =  new ArrayList <>()/* comment */;
  
  /* comment */ static 
  /* comment */ {
  /* comment */ }
  
  /** comment */
  int b01 = /* comment */ 0;
  String b02 = /* comment */ "";
  char b03 = /* comment */ ' ';
  /* comment */ private int b04; 
  /* comment */ @Deprecated int b05;
  /* comment */
  int b06 /* comment */, b07;
  int b08 /* comment */ = 0, b09;
  
  int/* comment */[] c01;
  int[/* comment */] c02;
  int[]/* comment */[] c03;
  int[] /* comment */ @A01 [] c04;
  int[] c05 /* comment*/ @A01 [];
  int[] c06 /* comment*/[];
  int[] c07 [/* comment*/];
  int[] c08 = /* comment */ new int[0];
  int[] c09 = new /* comment */ int[0];
  int[] c10 = new int /* comment */[0];
  int[] c11 = new int[/* comment */ 0];
  int[] c12 = new int[0/* comment */ ];
  int[] c13 = new int /* comment */ @A01 [0];
  int[] c14 = new int[] /* comment */{};
  int[] c15 = new int[] {/* comment */};
  int[] c16 = new int[] {/* comment */0, 1};
  int[] c17 = new int[] {0 /* comment separators of array initializers */, 1};
  int[][] c18 = new int[][] {/* comment */{0, 1}, null};
  int[][] c19 = new int[][] {{0 , 1}, /* comment */ null};
  
  /** comment */ abstract void m01();
  abstract /* comment */ @A01 void m02();
  abstract /* comment */ void m03();
  abstract void m04() /* comment */;
  void m05/* comment */() {}
  void m06(/* comment */) {}
  void m07() /* comment */{}
  void m08() { /* comment */}
  abstract int/* comment */[] m09() @A01 [];
  abstract int[/* comment */] m10() @A01 [];
  abstract int[] m11() /* comment */@A01 [];
  abstract int[] m12() @A01 /* comment */[];
  abstract int[] m13() @A01 [/* comment */];
  abstract void m14( /* comment */ int a);
  abstract void m14(long /* comment */ a);
  abstract void m14(short a/* comment */);
  abstract void m14(int /* comment */ ... a);
  abstract void m14(long /* comment */ @A01 ... a);
  abstract void m14(int a /* comment */, int b);
  abstract /* comment */<T> void m14(T t);
  abstract </* comment */T> void m15(T t);
  abstract <T/* comment */> void m16(T t);
  abstract void m17() /* comment */ throws Exception;
  abstract void m18() throws /* comment */ Exception;
  abstract void m19() throws RuntimeException /* comment separators of exceptions */, Exception;
  
  /* comment */ public Rest() {}
  public /* comment */ Rest(int i) {}
  public Rest /* comment */ (short i) {}
  public Rest(long i /* comment */ ) {}
  public Rest(char c ) /* comment */ {}
  public Rest(double d ) {/* comment */}
  
  Object rest(boolean test, List list, E2 e2, int[] array) throws Exception {
    /* comment */ assert test;
    assert /* comment */ test;
    assert test /* comment */;
    assert test /* comment */ : m09();
    assert test : /* comment */ m09();
    
    /* comment */ ;
    for (i/* comment */, j;;) {}
    /* comment */ for (;;) {
      /* comment */ break;
    }
    for /* comment */ (;;) {
      break /* comment */;
    }
    for ( /* comment */ ; ; ) { break;}
    for ( ; /* comment */ ; ) { break;}
    for ( ; ; /* comment */ ) { break;}
    for(;;) {
      if (test) {
        /* comment */ continue;
      } else if (test) {
        continue /* comment */ ;
      }
      break;
    }
    
    /* comment */ for (Object object : list) {}
    for /* comment */ (Object object : list) {}
    for( Object /* comment */ object : list) {}
    for( Object object /* comment */ : list) {}
    for( Object object : /* comment */ list) {}
    for( Object object : /* comment */ list) {}
    for( Object object : list/* comment */ ) {}
    
    /* comment */ if (test) {}
    if /* comment */ (test) {}
    if (/* comment */ test) {}
    if (test /* comment */ ) {}
    if (test) {} /* comment */  else {}
    
    /* comment */ do { } while(test);
    do { } /* comment */ while(test);
    do {} while /* comment */ (test);
    do {} while (test /* comment */);
    do {} while (test) /* comment */;
    
    /* comment */ while (test) {}
    while /* comment */ (test) {}
    while (/* comment */ test) {}
    while (test /* comment */) {}
    
    /* comment */ LABEL1: ;
    LABEL2 /* comment */ : ;
    
    /* comment */ switch(e2) {}
    switch /* comment */ (e2) {}
    switch (/* comment */ e2) {}
    switch (e2 /* comment */) {}
    switch (e2) /* comment */ {}
    switch (e2) {/* comment */ }
    switch (e2) {
      /* comment */ case A:
      case /* comment */ B:
      case C /* comment */:
      /* comment */ default:
    }
    switch (e2) { default/* comment */: }
    
    /* comment */ synchronized (list) {}
    synchronized /* comment */ (list) {}
    synchronized (/* comment */ list) {}
    synchronized (list /* comment */) {}
    
    if(test) {/* comment */ throw new Exception(); }
    if(test) {throw /* comment */ new Exception(); }
    if(test) {throw new /* comment */ Exception(); }
    if(test) {throw new Exception /* comment */(); }
    if(test) {throw new Exception (/* comment */); }
    if(test) {throw new Exception() /* comment */; }
    
    /* comment */ try { } catch (Exception e) {}
    try /* comment */ { } catch (Exception e) {}
    try { /* comment */ } catch (Exception e) {}
    try { } /* comment */ catch (Exception e) {}
    try { } catch /* comment */ (Exception e) {}
    try { } catch (/* comment */ Exception e) {}
    try { } catch (Exception /* comment */ e) {}
    try { } catch (Exception e /* comment */) {}
    try { } catch (Exception e ) /* comment */{}
    try { } catch (Exception e ) {/* comment */}
    try { throw new FileNotFoundException(); } catch (FileNotFoundException /* comment separators of unary types*/ | UnknownTypeException e ) {}
    try { } /* comment */ finally {}
    try { } finally /* comment */ {}
    try { } finally {/* comment */ }
    try /* comment */ (Closeable c = new FileInputStream("")) {}
    try (Closeable c = new FileInputStream("")/* comment */ ) {}
    try (Closeable c1 = new FileInputStream("") /* comment separators of resources */ ; Closeable c2 = new FileInputStream("")) {}
    
    int i /* comment */ = 0, j = 0;
    i /* comment */ ++;
    i /* comment */ -- ;
    /* comment */ --i;
    /* comment */ ++i;
    i /* comment */ += 1;
    i /* comment */ -= 1;
    i /* comment */ *= 1;
    i /* comment */ /= 1;
    i /* comment */ %= 1;
    i /* comment */ >>= 1;
    i /* comment */ <<= 1;
    i /* comment */ >>>= 1;
    i = /* comment */ ~1;
    i = /* comment */ +1;
    i = /* comment */ -1;
    i = +/* comment */+i;
    i = i /* comment */+ j;
    i = i + j /* comment */;
    i = i /* comment */ & 1;
    i = i /* comment */ | 1;
    i = i /* comment */ ^ 1;
    i /* comment */ &= 1;
    i /* comment */ |= 1;
    i /* comment */ ^= 1;
    i = /* comment */ array[0];
    i = array /* comment */[0];
    i = array[/* comment */ 0];
    i = array[0/* comment */ ];
    i = array[0]/* comment */;
    i = + + i;
    
    i = test /* comment */ ? i : j;
    i = test ? i /* comment */ : j;
    i = test ? i : j /* comment */;
    
    /* comment */((Object) list).toString();
    (/* comment */(Object) list).toString();
    ((/* comment */Object) list).toString();
    ((Object/* comment */) list).toString();
    ((Object) list/* comment */).toString();
    ((Object) list)/* comment */.toString();
    ((Object) list).toString/* comment */();
    ((Object) list).toString(/* comment */);
    ((Object) list).toString()/* comment */;

    test = /* comment */ !test;
    test = i /* comment */ > j;
    test = i /* comment */ < j;
    test = i /* comment */ <= j;
    test = i /* comment */ >= j;
    test = test /* comment */ && test;
    test = test /* comment */ || test;
    test = test /* comment */ ^ test;
    test = list /* comment */ instanceof Object;
    
    Class c;
    c = /* comment */int[].class;
    c = int/* comment */[].class;
    c = int[/* comment */].class;
    c = int[]/* comment */.class;
    c = int[]./* comment */class;
    c = int[].class/* comment */;
    
    I3 op;
    op = /* comment */(a, b) -> a - b;
    op = (/* comment */a, b) -> a - b;
    op = (a/* comment */, b) -> a - b;
    op = (a, /* comment */b) -> a - b;
    op = (a,b /* comment */) -> a - b;
    op = (a,b) /* comment */ -> a - b;
    op = (a,b) -> a - b /* comment */;
    
    String[] array2 = {"A", "B", "C"};
    Arrays.sort(array2 /* comment */, String::compareToIgnoreCase);
    Arrays.sort(array2, /* comment */ String::compareToIgnoreCase);
    Arrays.sort(array2, String/* comment */ ::compareToIgnoreCase);
    Arrays.sort(array2, String:: /* comment */compareToIgnoreCase);
    
    rest /* comment */(true, null, null, null);
    rest(/* comment */ true, null, null, null);
    rest(true /* comment */, null, null, null);
    
    if (test) { return null /* comment */; }
    /* comment */ return null;
  }
}

/* comment */
