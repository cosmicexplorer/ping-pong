scala_library(
  dependencies=[
    '3rdparty/jvm:finatra-thrift',
    '3rdparty/jvm:guice',
    'protocol',
  ]
)

jvm_binary(
  name='cli',
  main='pingpong.server.ServerMain',
  dependencies=[
    ':server',
  ],
)
