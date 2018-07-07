custom_scrooge_java_thrift_library(
  name='entities',
  sources=['entities.thrift'],
)

custom_scrooge_java_thrift_library(
  name='notifications',
  sources=['notifications.thrift'],
  dependencies=[
    ':entities',
  ],
  exports=[
    ':entities',
  ],
)

custom_scrooge_java_thrift_library(
  name='pingpong',
  sources=['pingpong.thrift'],
  dependencies=[
    ':notifications',
    ':repo_backend',
  ],
  exports=[
    ':notifications',
    ':repo_backend',
  ],
)

custom_scrooge_java_thrift_library(
  name='repo_backend',
  sources=['repo_backend.thrift'],
)

custom_scrooge_java_thrift_library(
  name='review_backend',
  sources=['review_backend.thrift'],
  dependencies=[
    ':pingpong',
  ],
  exports=[
    ':pingpong',
  ],
)
