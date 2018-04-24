#!/usr/bin/env python

# Copyright 2018 Informatics Matters Ltd.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import print_function
import os
from . import utils

# The root of the Service Descriptor file directory.
# Files are located in here within directories based on the name
# of the calling module, aka the`namespace`.
FILE_ROOT_ENV = 'FILE_UTILS_FILE_ROOT'
# Default sdf file extension
_SDF_EXT = '.sdf.gz'


def _get_file_root():
    """Returns the file root.
    A function to allow dynamic environment changes during testing -
    i.e. environment read during the calls rather than statically.
    """
    return os.environ.get(FILE_ROOT_ENV, '/root/sd-files')


def pick(file, namespace=None):
    """Returns the named file for the given namespace
    (the calling module's name). Files are expected to be located in
    `<_FILE_ROOT>/<namespace>`. If the file does not exist `None` is returned.

    If `xyx_module.py` makes a call to `pick` the namespace is `xyz_module`.

    :param file: The file, whose path is required.
    :type file: ``str``
    :param namespace: An optional namespace (sub-directory of the file root).
                      If not provided it is calculated automatically.
    :type namespace: ``str``
    :return: The full path to the file, or None if it does not exist
    :rtype: ``str``
    """
    if namespace is None:
        namespace = utils.get_undecorated_calling_module()
    file_path = os.path.join(_get_file_root(), namespace, file)
    return file_path if os.path.isfile(file_path) else None


def pick_sdf(file):
    """Returns a full path to the chosen SDF file. The supplied file
    is not expected to contain the SDF extension, this is added automatically.
    """
    return pick(file + _SDF_EXT,
                utils.get_undecorated_calling_module())
