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
    ':repo-backend',
  ],
  exports=[
    ':notifications',
    ':repo-backend',
  ],
)

custom_scrooge_java_thrift_library(
  name='repo-backend',
  sources=['repo_backend.thrift'],
)

custom_scrooge_java_thrift_library(
  name='review-backend',
  sources=['review_backend.thrift'],
  dependencies=[
    ':pingpong',
  ],
  exports=[
    ':pingpong',
  ],
)
