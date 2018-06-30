from __future__ import (absolute_import, division, generators, nested_scopes,
                        print_function, unicode_literals, with_statement)

from pants.backend.jvm.targets.jar_library import JarLibrary
from pants.backend.jvm.targets.scala_jar_dependency import ScalaJarDependency
from pants.java.jar.jar_dependency import JarDependency
from pants.util.objects import datatype


class InjectableScroogeDep(datatype([('name', unicode), ('klass', type)])):

  def __new__(cls, name, klass=ScalaJarDependency):
    return super(InjectableScroogeDep, cls).__new__(cls, name, klass)

  def as_custom_scrooge_jar_dep(self, version):
    return self.klass(org='com.twitter', name=self.name, rev=version)


class CustomScroogeDependencies(object):

  @classmethod
  def alias(cls):
    return 'custom_scrooge_dependencies'

  def __init__(self, parse_context):
    self._parse_context = parse_context

  _INJECTABLES_SPECS = {
    'scrooge-core': InjectableScroogeDep('scrooge-core'),
    'scrooge-gen': InjectableScroogeDep('scrooge-generator'),
    'scrooge-linter': InjectableScroogeDep('scrooge-linter'),
  }

  def _create_jar_library(self, name, jars):
    self._parse_context.create_object('jar_library', name=name, jars=jars)

  def _create_jar_library_from_deps(self, name, deps, version):
    all_jar_deps = [d.as_custom_scrooge_jar_dep(version) for d in deps]
    self._create_jar_library(name, all_jar_deps)

  def __call__(self, version):
    for name, dep in self._INJECTABLES_SPECS.items():
      # FIXME: do we need this deepcopy? it's done in managed_jar_dependencies.py but idk why
      self._create_jar_library_from_deps(name, [dep], version)
