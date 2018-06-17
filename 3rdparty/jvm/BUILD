jar_library(
  name='slf4j-impl',
  jars=[
    jar(org='ch.qos.logback', name='logback-classic', rev='1.2.3'),
  ],
)

jar_library(
  name='guice',
  jars=[
    jar(org='com.google.inject', name='guice', rev='4.2.0'),
  ],
)

jar_library(
  name='nailgun-server',
  jars=[
    jar(org='com.martiansoftware', name='nailgun-server', rev='0.9.1'),
  ],
)

TWITTER_JVM_REV = '18.6.0'

jar_library(
  name='finagle-thrift',
  jars=[
    jar(org='com.twitter', name='finagle-thrift_2.12', rev=TWITTER_JVM_REV,
        excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='finatra-thrift',
  jars=[
    jar(org='com.twitter', name='finatra-thrift_2.12', rev=TWITTER_JVM_REV,
        excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-core',
  jars=[
    jar(org='com.twitter', name='scrooge-core_2.12', rev=TWITTER_JVM_REV,
        excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-generator',
  jars=[
    jar(org='com.twitter', name='scrooge-generator_2.12', rev=TWITTER_JVM_REV,
        excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-linter',
  jars=[
    jar(org='com.twitter', name='scrooge-linter', rev=TWITTER_JVM_REV,
        excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='util-core',
  jars=[
    jar(org='com.twitter', name='util-core', rev=TWITTER_JVM_REV),
  ],
)

jar_library(
  name='libthrift-0.6.1',
  jars=[
    jar(org='org.apache.thrift', name='libthrift', rev='0.6.1'),
  ],
)
