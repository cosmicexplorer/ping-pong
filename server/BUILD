scala_library(
  dependencies=[
    '3rdparty/jvm:finatra-thrift',
    '3rdparty/jvm:guice',
    '3rdparty/jvm:slf4j-impl',
    'protocol:repo_backend',
  ]
)

jvm_binary(
  name='cli',
  main='pingpong.server.ServerMain',
  dependencies=[
    ':server',
  ],
)
