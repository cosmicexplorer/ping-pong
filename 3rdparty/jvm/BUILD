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

jar_library(
  name='libthrift',
  jars=[
    jar(org='org.apache.thrift', name='libthrift', rev='0.11.0'),
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
    scala_jar(org='com.twitter', name='util-core', rev=TWITTER_JVM_REV,
              excludes=[exclude(org='org.scala-lang', name='scala-library'),
                        exclude(org='org.scala-lang', name='scala-reflect')]),
  ],
  dependencies=[
    '//:scala-library',
    '//:scala-reflect',
  ],
)

ASM_REV = '6.1.1'

jar_library(
  name='ow2-asm',
  jars=[
    jar(org='org.ow2.asm', name='asm', rev=ASM_REV),
    jar(org='org.ow2.asm', name='asm-analysis', rev=ASM_REV),
    jar(org='org.ow2.asm', name='asm-commons', rev=ASM_REV),
    jar(org='org.ow2.asm', name='asm-tree', rev=ASM_REV),
    jar(org='org.ow2.asm', name='asm-util', rev=ASM_REV),
  ],
)

ENSIME_REV = '2.0.1'

# TODO: make this into its own subsystem!
jar_library(
  name='ensime-api',
  jars=[
    scala_jar(org='org.ensime', name='api', rev=ENSIME_REV,
              excludes=[exclude(org='org.scala-lang'),
                        exclude(org='org.ow2.asm'),
                        exclude(org='io.spray')]),
    scala_jar(org='org.ensime', name='core', rev=ENSIME_REV,
              excludes=[exclude(org='org.scala-lang'),
                        exclude(org='org.ow2.asm'),
                        exclude(org='io.spray')]),
    scala_jar(org='org.ensime', name='s-express', rev=ENSIME_REV,
              excludes=[exclude(org='org.scala-lang'),
                        exclude(org='org.ow2.asm'),
                        exclude(org='io.spray')]),
    scala_jar(org='org.ensime', name='util', rev=ENSIME_REV,
              excludes=[exclude(org='org.scala-lang'),
                        exclude(org='org.ow2.asm'),
                        exclude(org='io.spray')]),
    # We pull everything in intransitively because the version of spray-json used is different than
    # the one we want, but we then need to provide scalaz-ioeffect for the 'api' jar, and others for
    # others.
    scala_jar(org='com.chuusai', name='shapeless', rev='2.3.3'),
    # scala_jar(org='com.lihaoyi', name='fastparse', rev='0.4.4'),
    scala_jar(org='org.scalaz', name='scalaz-ioeffect', rev='2.5.0'),
  ],
  dependencies=[
    '//:scalap',
    ':ow2-asm',
  ],
)

# # FIXME: use this along with sbt_dist()!
# jar_library(
#   name='ensime-server-2.0.1-local',
#   jars=[
#     scala_jar(org='org.ensime', name='server', rev='2.0.1-EDITED',
#               excludes=[exclude(org='org.scala-lang')]),
#     jar(org='org.scala-lang', name='scalap', rev='2.12.6'),
#   ],
#   dependencies=[
#     # Just so this gets invalidated if the local sbt dist does too.
#     '3rdparty/jvm/vendor/ensime-server',
#   ],
# )

jar_library(
  name='ensime-server',
  jars=[
    scala_jar(org='org.ensime', name='server', rev=ENSIME_REV,
              excludes=[exclude(org='org.scala-lang'),
                        exclude(org='org.ow2.asm')]),
    jar(org='org.scala-lang', name='scalap', rev='2.12.6'),
    # We pull everything in intransitively because the version of spray-json used is different than
    # the one we want, but we then need to provide scalaz-ioeffect for the 'api' jar, and others for
    # others.
    scala_jar(org='com.chuusai', name='shapeless', rev='2.3.3'),
    # scala_jar(org='com.lihaoyi', name='fastparse', rev='0.4.4'),
    scala_jar(org='org.scalaz', name='scalaz-ioeffect', rev='2.5.0'),
  ],
  dependencies=[
    '//:scala-library',
    '//:scala-reflect',
    ':ow2-asm',
  ]
)

jar_library(
  name='spray-json',
  jars=[
    scala_jar(org='io.spray', name='spray-json', rev='1.3.4'),
  ],
)

jar_library(
  name='ammonite-ops',
  jars=[
    scala_jar(org='com.lihaoyi', name='ammonite-ops', rev='1.1.2'),
  ],
)
