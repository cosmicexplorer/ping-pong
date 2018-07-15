from __future__ import (absolute_import, division, generators, nested_scopes,
                        print_function, unicode_literals, with_statement)

import os

from pants.base.build_environment import get_pants_cachedir
from pants.option.custom_types import dir_option
from pants.subsystem.subsystem import Subsystem
from pants.util.memo import memoized_property


class Sbt(Subsystem):

  options_scope = 'sbt'

  @classmethod
  def register_options(cls, register):
    super(Sbt, cls).register_options(register)

    register('--local-publish-repo', type=dir_option,
             default=os.path.expanduser('~/.ivy2'),
             advanced=True,
             help='Where to publish locally-built sbt distributions to.')

    register('--version', type=str, default=None, advanced=True,
             help='Optional version of sbt to use to build local sbt dists.')

  @memoized_property
  def local_publish_repo(self):
    return self.get_options().local_publish_repo

  @memoized_property
  def version(self):
    return self.get_options().version
