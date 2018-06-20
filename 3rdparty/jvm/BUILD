jar_library(
  name='slf4j-impl',
  jars=[
    jar(org='ch.qos.logback', name='logback-classic', rev='1.2.3'),
  ],
)

GOOGLE_GUICE_REV = '4.2.0'

jar_library(
  name='guice',
  jars=[
    jar(org='com.google.inject', name='guice', rev=GOOGLE_GUICE_REV),
  ],
)

jar_library(
  name='guice-test-jars',
  jars=[
    jar(org='com.google.inject.extensions', name='guice-testlib', rev=GOOGLE_GUICE_REV),
  ],
  scope='compile test',
)


TWITTER_JVM_REV = '18.6.0'

jar_library(
  name='finatra-thrift',
  jars=[
    scala_jar(org='com.twitter', name='finatra-thrift', rev=TWITTER_JVM_REV),
  ],
)

jar_library(
  name='finatra-thrift-test-jars',
  jars=[
    scala_jar(org='com.twitter', name='finatra-thrift', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-server', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='finatra-jackson', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='finatra-utils', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-app', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-core', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-modules', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-request-scope', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-server', rev=TWITTER_JVM_REV,
              classifier='tests'),
    scala_jar(org='com.twitter', name='inject-utils', rev=TWITTER_JVM_REV,
              classifier='tests'),
  ],
  dependencies=[
    ':mockito',
    ':slf4j-impl',
  ],
  scope='compile test',
)

jar_library(
  name='scrooge-core',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-core', rev=TWITTER_JVM_REV),
  ],
)


SCALA_REV = '2.12.6'

jar_library(
  name='scala-compiler',
  jars=[
    jar(org='org.scala-lang', name='scala-compiler', rev=SCALA_REV),
  ]
)

jar_library(
  name='scala-library',
  jars=[
    jar(org='org.scala-lang', name='scala-library', rev=SCALA_REV),
  ]
)

jar_library(
  name='scala-repl',
  jars=[
    scala_jar(org='org.scala-lang', name='scala-repl', rev=SCALA_REV),
  ]
)

jar_library(
  name='scala-runtime',
  jars=[
    scala_jar(org='org.scala-lang', name='scala-runtime', rev=SCALA_REV),
  ]
)

jar_library(
  name='mockito',
  jars=[
    jar(org='org.mockito', name='mockito-core', rev='1.9.5'),
    jar(org='org.mockito', name='mockito-core', rev='1.9.5', classifier='test'),
  ],
  scope='compile test',
)

jar_library(
  name='scalatest',
  jars=[
    scala_jar(org='org.scalatest', name='scalatest', rev='3.0.5'),
    scala_jar(org='org.scalatest', name='scalatest', rev='3.0.5', classifier='test'),
  ],
  scope='compile test',
)
