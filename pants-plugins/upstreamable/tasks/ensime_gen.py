from __future__ import (absolute_import, division, generators, nested_scopes, print_function,
                        unicode_literals, with_statement)

import json
import os

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
from pants.util.memo import memoized_property
from pants.util.objects import SubclassesOf

from upstreamable.tasks.bootstrap_ensime_gen import EnsimeGenJar


class EnsimeGen(ExportTask, JvmToolTaskMixin):

  @classmethod
  def prepare(cls, options, round_manager):
    round_manager.require_data(EnsimeGenJar)
    cls.prepare_tools(round_manager)

  @classmethod
  def subsystem_dependencies(cls):
    return super(EnsimeGen, cls).subsystem_dependencies() + (DistributionLocator,)

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

        with open(export_outfile, 'rb') as inf:
          execute_java(ensime_gen_classpath,
                       'pingpong.ensime.EnsimeFileGen',
                       workunit_name='ensime-gen',
                       workunit_labels=[WorkUnitLabel.TOOL],
                       distribution=DistributionLocator.cached(),
                       stdin=inf)
