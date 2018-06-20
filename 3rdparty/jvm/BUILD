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
    scala_jar(org='com.twitter', name='finatra-thrift', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache.thrift', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.11.0',
  ],
)

jar_library(
  name='finatra-thrift-test-jars',
  jars=[
    scala_jar(org='com.twitter', name='finatra-thrift', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-server', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='finatra-jackson', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='finatra-utils', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-app', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-core', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-modules', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-request-scope', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-server', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
    scala_jar(org='com.twitter', name='inject-utils', rev=TWITTER_JVM_REV,
              classifier='tests',
              excludes=[
                exclude(org='org.apache.thrift', name='libthrift'),
                exclude(org='org.mockito'),
              ]),
  ],
  dependencies=[
    ':libthrift-0.11.0',
    ':mockito',
  ],
  scope='compile test',
)

jar_library(
  name='scrooge-core',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-core', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache.thrift', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-generator',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-generator', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache.thrift', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='scrooge-linter',
  jars=[
    scala_jar(org='com.twitter', name='scrooge-linter', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.apache.thrift', name='libthrift')]),
  ],
  dependencies=[
    ':libthrift-0.6.1',
  ],
)

jar_library(
  name='libthrift-0.6.1',
  jars=[
    jar(org='org.apache.thrift', name='libthrift', rev='0.6.1'),
  ],
)

jar_library(
  name='libthrift-0.11.0',
  jars=[
    jar(org='org.apache.thrift', name='libthrift', rev='0.11.0'),
  ],
  scope='compile test',
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
