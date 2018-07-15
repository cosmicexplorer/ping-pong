from __future__ import (absolute_import, division, generators, nested_scopes,
                        print_function, unicode_literals, with_statement)

from pants.backend.codegen.thrift.java.thrift_defaults import ThriftDefaults
from pants.contrib.scrooge.tasks.scrooge_gen import ScroogeGen
from pants.util.memo import memoized_property
from pants.util.objects import Exactly
from upstreamable.targets.custom_scrooge_java_thrift_library import \
    CustomScroogeJavaThriftLibrary


class CustomScrooge(ScroogeGen):

  source_target_constraint = Exactly(CustomScroogeJavaThriftLibrary)

  @classmethod
  def subsystem_dependencies(cls):
    return super(CustomScrooge, cls).subsystem_dependencies() + (
      # Have to use the global version, scoped "doesn't work".
      ThriftDefaults,
    )

  @memoized_property
  def _thrift_defaults_subsystem(self):
    return ThriftDefaults.global_instance()

  def _use_custom_scrooge(self, target):
    # We only handle requests for 'custom-scrooge' compilation and not, e.g. 'scrooge' or 'thrift'
    return self._thrift_defaults_subsystem.compiler(target) == 'custom-scrooge'

  def is_gentarget(self, target):
    return self.source_target_constraint.satisfied_by(target) and self._use_custom_scrooge(target)
