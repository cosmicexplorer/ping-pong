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
    # FIXME: we can't get the transitive closure of all the deps for test jars (this is a known
    # issue with classifiers in general, see
    # https://twitter.github.io/finatra/index.html#test-dependencies). This is almost definitely
    # something that could/should be added to coursier, because I think it would require traversing
    # the dependency set of the test jars, and we probably don't want to do that ourselves.
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
)


jar_library(
  name='mockito',
  jars=[
    jar(org='org.mockito', name='mockito-core', rev='1.9.5'),
    jar(org='org.mockito', name='mockito-core', rev='1.9.5', classifier='test'),
  ],
)

jar_library(
  name='scalatest',
  jars=[
    scala_jar(org='org.scalatest', name='scalatest', rev='3.0.5'),
    scala_jar(org='org.scalatest', name='scalatest', rev='3.0.5', classifier='test'),
  ],
)

jar_library(
  name='twitter-util',
  jars=[
    scala_jar(org='com.twitter', name='util-core', rev=TWITTER_JVM_REV),
  ]
)