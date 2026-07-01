package checks;

import com.datastax.oss.quarkus.runtime.api.mapper.Mapper;

class MapperWithoutDaoFactoryCheckSample {

  // Case with unresolved supertype - should not raise issue to avoid false positive
  @Mapper
  interface MapperWithUnresolvedSupertype extends UnresolvedInterface {
  }

  // Case with partially resolved inheritance
  @Mapper
  interface MapperExtendingResolvableAndUnresolvable extends ResolvableInterface, AnotherUnresolvedInterface {
  }

  interface ResolvableInterface {
    String getName();
  }
}
