from __future__ import (absolute_import, division, generators, nested_scopes,
                        print_function, unicode_literals, with_statement)

from pants.backend.jvm.targets.jar_library import JarLibrary
from pants.backend.jvm.targets.scala_jar_dependency import ScalaJarDependency
from pants.build_graph.address import Address
from pants.build_graph.injectables_mixin import InjectablesMixin
from pants.subsystem.subsystem import Subsystem
from pants.util.memo import memoized_property


class CustomScroogeBootstrap(Subsystem, InjectablesMixin):

  options_scope = 'custom-scrooge-bootstrap'

  @classmethod
  def register_options(cls, register):
    super(CustomScroogeBootstrap, cls).register_options(register)

    register('--tool-version', type=str, default='18.6.0', advanced=True,
             help='The version to use for the scrooge JVM dependencies.')

  @memoized_property
  def _tool_version(self):
    return self.get_options().tool_version

  _INJECTABLES_SPECS = {
    'scrooge-core': 'scrooge-core',
    'scrooge-gen': 'scrooge-generator',
    'scrooge-linter': 'scrooge-linter',
  }

  def injectables(self, build_graph):
    for spec_key, jar_name in self._INJECTABLES_SPECS.items():
      spec = self.injectables_spec_for_key(spec_key)
      target_address = Address.parse(spec)
      # FIXME: this is never going to be true if we set thrift-defaults.compiler to
      # 'custom-scrooge', because the address is synthesized already for the purposes of the normal
      # scrooge task.
      if build_graph.contains_address(target_address):
        if not build_graph.get_target(target_address).is_synthetic:
          raise build_graph.ManualSyntheticTargetError(target_address)
      else:
        scala_jar_dep = ScalaJarDependency(org='com.twitter', name=jar_name, rev=self._tool_version)
        build_graph.inject_synthetic_target(
          target_address, JarLibrary, jars=[scala_jar_dep])

  @property
  def injectables_spec_mapping(self):
    ret = super(CustomScroogeBootstrap, self).injectables_spec_mapping.copy()

    for spec_key in self._INJECTABLES_SPECS:
      ret[spec_key] = ['//:{}'.format(spec_key)]

    return ret
