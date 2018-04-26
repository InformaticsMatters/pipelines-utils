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

# Files are normally located in sub-directories of the pipeline module path.
# For example a pipeline module 'pipeline_a.py'  that expects to use a file
# or SDF picker would place its files in the directory
# 'pipelines/demo/pipeline_a'.


def pick(filename, directory=None):
    """Returns the named file. If directory is not specified the file is
    expected to be located in a sub-directory whose name matches
    that of the calling module otherwise the file is expected to be found in
    the named directory.

    :param filename: The file, whose path is required.
    :type filename: ``str``
    :param directory: An optional directory.
                      If not provided it is calculated automatically.
    :type directory: ``str``
    :return: The full path to the file, or None if it does not exist
    :rtype: ``str``
    """
    if directory is None:
        directory = utils.get_undecorated_calling_module()
        # If the 'cwd' is not '/output' (which indicates we're in a Container)
        # then remove the CWD and the anticipated '/'
        # from the front of the module
        if os.getcwd() not in ['/output']:
            directory = directory[len(os.getcwd()) + 1:]

    file_path = os.path.join(directory, filename)
    return file_path if os.path.isfile(file_path) else None


def pick_sdf(filename, directory=None):
    """Returns a full path to the chosen SDF file. The supplied file
    is not expected to contain a recognised SDF extension, this is added
    automatically.
    If a file with the extension `.sdf.gz` or `.sdf` is found the path to it
    (excluding the extension) is returned. If this fails, `None` is returned.

    :param filename: The SDF file basename, whose path is required.
    :type filename: ``str``
    :param directory: An optional directory.
                      If not provided it is calculated automatically.
    :type directory: ``str``
    :return: The full path to the file without extension,
             or None if it does not exist
    :rtype: ``str``
    """
    if directory is None:
        directory = utils.get_undecorated_calling_module()
        # If the 'cwd' is not '/output' (which indicates we're in a Container)
        # then remove the CWD and the anticipated '/'
        # from the front of the module
        if os.getcwd() not in ['/output']:
            directory = directory[len(os.getcwd()) + 1:]

    file_path = os.path.join(directory, filename)
    if os.path.isfile(file_path + '.sdf.gz'):
        return file_path + '.sdf.gz'
    elif os.path.isfile(file_path + '.sdf'):
        return file_path + '.sdf'
    # Couldn't find a suitable SDF file
    return None
