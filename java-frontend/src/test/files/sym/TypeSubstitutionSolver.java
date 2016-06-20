class A<A1> implements I<A1>, L<String> {
  A<A1> aX;
}
class B<B1, B2> implements I<B1>, K<B2> {
  B<B1,B2> bXY;
}
class C<C1, C2> implements J<C1, C2> {
  C<C1,C2> cXY;
}
class D<D1> extends A<D1> {
  D<D1> dX;
}
class E<E1, E2> extends D<E1> implements K<E2>, M<E1, E2> {
  E<E1, E2> eXY;
}
class F<F1, F2, F3, V4> extends B<F2, F3> implements N<F1, V4> {
  F<F1, F2, F3, V4> fWXYZ;
}
class G<G1> extends A<G1> implements N<G1, String> {
  G<G1> gX;
}

interface NonParametrizedInterface {}
interface I<I1> extends NonParametrizedInterface {}
interface J<J1, J2> {}
interface K<K1> extends L<K1> {}
interface L<L1> {}
interface M<M1, M2> extends J<M1, M2> {}
interface N<N1, N2> extends O<N2>, M<N1, N2> {}
interface O<O1> {}

class testClass {
  String string;
  Integer integer;
  Number number;

  I<String> iString;
  K<Integer> kInteger;
  L<Number> lNumber;
  J<String, Integer> jStringInteger;
  A<String> aString;
}

class Recurs<T, S extends T>  {
  Recurs<Object, Boolean> inst = new Recurs<>(Boolean.class);
  public Recurs(Class<S> requiredClass) {
  }
}
