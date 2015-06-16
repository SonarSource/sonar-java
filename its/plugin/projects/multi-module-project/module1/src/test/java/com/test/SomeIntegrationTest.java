package com.test;

import java.lang.IllegalStateException;

public class SomeIntegrationTest {

  void myIntegrationTest() {
    if(1+2==4) {
      throw new IllegalStateException("arithmetic as we know it has changed.");
    }
  }

}