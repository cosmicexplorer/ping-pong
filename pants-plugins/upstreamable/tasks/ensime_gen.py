from __future__ import (absolute_import, division, generators, nested_scopes, print_function,
                        unicode_literals, with_statement)

import json
import os

from pants.base.build_environment import get_buildroot, get_pants_cachedir
from pants.backend.jvm.subsystems.scala_platform import ScalaPlatform
from pants.backend.jvm.tasks.jvm_tool_task_mixin import JvmToolTaskMixin
from pants.backend.project_info.tasks.export import ExportTask
from pants.build_graph.address import Address
from pants.base.workunit import WorkUnitLabel
from pants.java.distribution.distribution import DistributionLocator
from pants.java.executor import SubprocessExecutor
from pants.java.util import execute_java
from pants.option.custom_types import target_option
from pants.util.collections import assert_single_element
from pants.util.contextutil import temporary_dir
from pants.util.dirutil import safe_mkdir
from pants.util.memo import memoized_property
from pants.util.objects import SubclassesOf

from upstreamable.tasks.bootstrap_ensime_gen import EnsimeGenJar


class EnsimeGen(ExportTask, JvmToolTaskMixin):

  @classmethod
  def register_options(cls, register):
    super(EnsimeGen, cls).register_options(register)

    register('--reported-scala-version', type=str, default=None,
             help='Scala version to report to ensime. Defaults to the scala platform version.')
    register('--scalac-options', type=list,
             default=['-deprecation', '-unchecked', '-Xlint'],
             help='Options to pass to scalac for ensime.')
    register('--javac-options', type=list,
             default=['-deprecation', '-Xlint:all', '-Xlint:-serial', '-Xlint:-path'],
             help='Options to pass to javac for ensime.')

  @classmethod
  def prepare(cls, options, round_manager):
    round_manager.require_data(EnsimeGenJar)
    cls.prepare_tools(round_manager)

  @classmethod
  def subsystem_dependencies(cls):
    return super(EnsimeGen, cls).subsystem_dependencies() + (DistributionLocator, ScalaPlatform,)

  def _make_ensime_cache_dir(self):
    bootstrap_dir = get_pants_cachedir()
    cache_dir = os.path.join(bootstrap_dir, 'ensime')
    safe_mkdir(cache_dir)
    return cache_dir

  def execute(self):

    exported_targets_map = self.generate_targets_map(self.context.targets())
    export_result = json.dumps(exported_targets_map, indent=4, separators=(',', ': '))

    with temporary_dir() as tmpdir:
      export_outfile = os.path.join(tmpdir, 'export-out.json')
      with open(export_outfile, 'wb') as outf:
        outf.write(export_result)

      with self.context.new_workunit(
          name='ensime-gen-invoke',
          labels=[WorkUnitLabel.TOOL],
      ) as workunit:

        ensime_gen_jar = self.context.products.get_data(EnsimeGenJar)
        ensime_gen_classpath = [ensime_gen_jar.tool_jar_path]

        # TODO: use JvmPlatform for jvm options!
        reported_scala_version = self.get_options().reported_scala_version
        if not reported_scala_version:
          reported_scala_version = ScalaPlatform.global_instance().version

        argv = [
          get_buildroot(),
          reported_scala_version,
          self._make_ensime_cache_dir(),
        ]

        with open(export_outfile, 'rb') as inf:
          execute_java(ensime_gen_classpath,
                       'pingpong.ensime.EnsimeFileGen',
                       args=argv,
                       workunit_name='ensime-gen',
                       workunit_labels=[WorkUnitLabel.TOOL],
                       distribution=DistributionLocator.cached(),
                       stdin=inf)
