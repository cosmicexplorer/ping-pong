from __future__ import (absolute_import, division, generators, nested_scopes,
                        print_function, unicode_literals, with_statement)

from pants.build_graph.build_file_aliases import BuildFileAliases
from pants.goal.task_registrar import TaskRegistrar as task

from upstreamable.subsystems.sbt import Sbt
from upstreamable.targets.custom_scala_dependencies import CustomScalaDependencies
from upstreamable.targets.custom_scrooge_dependencies import CustomScroogeDependencies
from upstreamable.targets.custom_scrooge_java_thrift_library import CustomScroogeJavaThriftLibrary
from upstreamable.targets.sbt_dist import SbtDist
from upstreamable.tasks.custom_scrooge import CustomScrooge
from upstreamable.tasks.bootstrap_ensime_gen import BootstrapEnsimeGen
from upstreamable.tasks.ensime_gen import EnsimeGen
from upstreamable.tasks.publish_local_sbt_distributions import PublishLocalSbtDistributions


def build_file_aliases():
  return BuildFileAliases(
    targets={
      CustomScroogeJavaThriftLibrary.alias(): CustomScroogeJavaThriftLibrary,
      SbtDist.alias(): SbtDist,
    },
    context_aware_object_factories={
      CustomScalaDependencies.alias(): CustomScalaDependencies,
      CustomScroogeDependencies.alias(): CustomScroogeDependencies,
    },
  )


def global_subsystems():
  return {Sbt}

def register_goals():
  task(name='publish-local-sbt-distributions', action=PublishLocalSbtDistributions).install('bootstrap')
  task(name='bootstrap-ensime-gen', action=BootstrapEnsimeGen).install('bootstrap')
  task(name='custom-scrooge', action=CustomScrooge).install('gen')
  task(name='ensime-gen', action=EnsimeGen).install('ensime')
