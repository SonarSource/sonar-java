
class UsingLambda {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UsingLambda.class);

  void start(int port) {

    unknown((a, b) -> {
      LOG.info(a.foo());
    });

  }
}
