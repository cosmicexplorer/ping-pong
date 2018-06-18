jar_library(
  name='commons-lang',
  jars=[
    jar(org='commons-lang', name='commons-lang', rev='2.5'),
  ],
)

jar_library(
  name='guice',
  jars=[
    jar(org='com.google.inject', name='guice', rev='4.2.0'),
  ],
)

TWITTER_JVM_REV = '18.6.0'

jar_library(
  name='finagle-thrift',
  jars=[
    scala_jar(org='com.twitter', name='finagle-thrift', rev=TWITTER_JVM_REV,
              # classifier='test',
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
  scope='compile test',
)

jar_library(
  name='finatra-thrift',
  jars=[
    scala_jar(org='com.twitter', name='finatra-thrift', rev=TWITTER_JVM_REV,
              excludes=[
                exclude(org='com.twitter', name='finagle-thrift'),
                exclude(org='org.apache', name='libthrift'),
              ]),
  ],
  dependencies=[
    ':finagle-thrift',
    ':thrift-0.6.1',
  ],
  scope='compile test',
)

jar_library(
  name='finatra-thrift-test',
  jars=[
    scala_jar(org='com.twitter', name='finatra-thrift', rev=TWITTER_JVM_REV,
              classifier='test',
              excludes=[
                exclude(org='com.twitter', name='finagle-thrift'),
                exclude(org='org.apache', name='libthrift'),
              ]),
  ],
  dependencies=[
    ':finagle-thrift',
    ':thrift-0.6.1',
  ],
  scope='compile test',
)

jar_library(
  name='inject-server',
  jars=[
    scala_jar(org='com.twitter', name='inject-server', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
  scope='compile test',
)

jar_library(
  name='inject-thrift',
  jars=[
    scala_jar(org='com.twitter', name='inject-thrift', rev=TWITTER_JVM_REV,
              classifier='test',
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
  scope='compile test',
)

jar_library(
  name='inject-thrift-client',
  jars=[
    scala_jar(org='com.twitter', name='inject-thrift-client', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
  scope='compile test',
)

jar_library(
  name='scrooge-core',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-core', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-generator',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-generator', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-linter',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-linter', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache', name='libthrift')]),
  ],
  dependencies=[
    ':thrift-0.6.1',
  ],
)

jar_library(
  name='libthrift-0.6.1',
  jars=[
    jar(org='org.apache.thrift', name='libthrift', rev='0.6.1'),
  ],
)

target(
  name='thrift-0.6.1',
  dependencies=[
    ':commons-lang',
    ':libthrift-0.6.1',
    ':slf4j-api',
  ]
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
  name='slf4j-api',
  jars=[
    jar(org='org.slf4j', name='slf4j-api', rev='1.6.1'),
  ],
)
