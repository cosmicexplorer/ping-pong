jar_library(
  name='guice',
  jars=[
    jar(org='com.google.inject', name='guice', rev='4.2.0'),
  ],
)

jar_library(
  name='finatra-http',
  jars=[
    # FIXME: why do I have to manually specify "_2.12" here???
    jar(org='com.twitter', name='finatra-http_2.12', rev='18.6.0'),
    # FIXME: there's an error here where something somewhere is asking for version 0.0.9 of this jar
    # -- why (???)
    jar(org='com.twitter.common', name='metrics', rev='0.0.39'),
    # FIXME: why do i have to do this? This is why we have oss.sonatype.org in our coursier repos.
    jar(org='org.apache.thrift', name='libthrift', rev='0.6.1'),
  ],
)

jar_library(
  # GraphQL library
  name='sangria',
  jars=[
    jar(org='org.sangria-graphql', name='sangria_2.12', rev='1.4.1'),
  ],
)

jar_library(
  name='sangria-json4s-jackson',
  jars=[
    jar(org='org.sangria-graphql', name='sangria-json4s-jackson_2.12', rev='1.0.0'),
  ]
)

jar_library(
  name='slf4j',
  jars=[
    jar(org='org.slf4j', name='slf4j-simple', rev='1.7.21'),
  ],
)
