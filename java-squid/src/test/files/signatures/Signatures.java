class Signatures {
  protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {}
  protected Collection<Thread> getQueuedThreads() {}

  public Object instantiate(String className, ObjectName loaderName,
    Object params[], String signature[])
    throws ReflectionException, MBeanException, InstanceNotFoundException {}

  public List<ObjectName> getRole(String roleName)
    throws IllegalArgumentException,
    RoleNotFoundException,
    RelationServiceNotRegisteredException {}

  public Object invoke(Object obj, Object... args) {}
}
