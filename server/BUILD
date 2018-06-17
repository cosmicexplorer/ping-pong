scala_library(
  dependencies=[
    '3rdparty/jvm:finatra-http',
    '3rdparty/jvm:guice',
    '3rdparty/jvm:slf4j',
  ]
)

jvm_binary(
  name='cli',
  main='pingpong.server.ServerMain',
  dependencies=[
    ':server',
  ],
)
