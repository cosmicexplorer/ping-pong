from __future__ import (absolute_import, division, generators, nested_scopes, print_function,
                        unicode_literals, with_statement)

import glob
import os
import shutil

from pants.base.exceptions import TaskError
from pants.base.workunit import WorkUnit, WorkUnitLabel
from pants.task.task import Task
from pants.option.custom_types import target_option
from pants.util.collections import assert_single_element
from pants.util.contextutil import temporary_dir
from pants.util.memo import memoized_property
from pants.util.objects import datatype
from pants.util.process_handler import subprocess


class EnsimeGenJar(datatype(['tool_jar_path'])): pass


class BootstrapEnsimeGen(Task):

  @classmethod
  def product_types(cls):
    return [EnsimeGenJar]

  @classmethod
  def register_options(cls, register):
    super(BootstrapEnsimeGen, cls).register_options(register)

    register('--skip', type=bool, advanced=True,
             help='Whether to skip this task (e.g. if we are currently executing the subprocess).')

    register('--ensime-gen-binary', type=target_option, default='//:ensime-gen', advanced=True,
             help='jvm_binary() target to execute to interpret export json into an ensime project.')

  @memoized_property
  def _binary_tool_target(self):
    return self.get_options().ensime_gen_binary

  @memoized_property
  def _bootstrap_config_files(self):
    return self.get_options().pants_config_files + ['pants.ini.bootstrap']

  class BootstrapEnsimeError(TaskError): pass

  def _collect_dist_jar(self, dist_dir):
    # We should only see a single file in the dist dir.
    dist_jar_glob = os.path.join(dist_dir, '*.jar')
    globbed_jars = glob.glob(dist_jar_glob)

    if globbed_jars:
      return assert_single_element(globbed_jars)
    else:
      return None

  def _build_binary(self, ensime_binary_target_spec):

    with temporary_dir() as tmpdir:
      cmd = [
        './pants',
        '--pants-config-files=[{}]'.format(', '.join('"{}"'.format(f) for f in self._bootstrap_config_files)),
        '--pants-distdir={}'.format(tmpdir),
        '--bootstrap-ensime-gen-skip',
        'binary',
        ensime_binary_target_spec,
      ]

      env = os.environ.copy()

      # TODO: replace space join with safe_shlex_join() when #5493 is merged!
      with self.context.new_workunit(
          name='bootstrap-ensime-gen-subproc', cmd=' '.join(cmd), labels=[WorkUnitLabel.BOOTSTRAP],
      ) as workunit:

        try:
          subprocess.check_call(cmd,
                                stdout=workunit.output('stdout'),
                                stderr=workunit.output('stderr'),
                                env=env)
        except (OSError, subprocess.CalledProcessError) as e:
          workunit.set_outcome(WorkUnit.FAILURE)
          raise self.BootstrapEnsimeError(
            "Error bootstrapping ensime-gen jvm sources with command {} from target {}."
            .format(cmd, ensime_binary_target_spec),
            e)

      dist_jar = self._collect_dist_jar(tmpdir)
      jar_fname = os.path.basename(dist_jar)
      cached_jar_path = os.path.join(self.workdir, jar_fname)
      shutil.move(dist_jar, cached_jar_path)

  def execute(self):

    if self.get_options().skip:
      return

    ensime_binary_target_spec = self._binary_tool_target
    self._build_binary(ensime_binary_target_spec)
    built_jar = self._collect_dist_jar(self.workdir)

    self.context.products.register_data(EnsimeGenJar, EnsimeGenJar(built_jar))
