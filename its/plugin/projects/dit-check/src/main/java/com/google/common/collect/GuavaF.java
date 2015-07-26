package com.google.common.collect;

public abstract class GuavaF<C extends Comparable> extends ContiguousSet<C> {

  GuavaF(DiscreteDomain<C> domain) {
    super(domain);
  }

}
