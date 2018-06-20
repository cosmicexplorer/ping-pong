from __future__ import (absolute_import, division, generators, nested_scopes,
                        print_function, unicode_literals, with_statement)

from pants.contrib.scrooge.tasks.scrooge_gen import ScroogeGen
from pants.build_graph.build_file_aliases import BuildFileAliases
from pants.goal.task_registrar import TaskRegistrar as task

from upstreamable.subsystems.custom_scrooge_bootstrap import CustomScroogeBootstrap
from upstreamable.targets.custom_scrooge_java_thrift_library import CustomScroogeJavaThriftLibrary
from upstreamable.tasks.custom_scrooge import CustomScrooge


def build_file_aliases():
  return BuildFileAliases(
    targets={
      CustomScroogeJavaThriftLibrary.alias(): CustomScroogeJavaThriftLibrary,
    },
  )


def global_subsystems():
  return {CustomScroogeBootstrap}


def register_goals():
  task(name='custom-scrooge', action=CustomScrooge).install('gen', before=ScroogeGen)
