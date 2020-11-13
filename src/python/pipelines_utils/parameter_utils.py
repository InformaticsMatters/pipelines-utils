#!/usr/bin/env python

# Copyright 2017 Informatics Matters Ltd.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR conditions OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""parameter_utils.py

A number of utilities relating to the splitting (normalising) of
parameter values into lists/tuples.
"""


def add_default_input_args(parser):
    parser.add_argument('-i', '--input',
                        help="Input file, if not defined the STDIN is used")
    parser.add_argument('-if', '--informat', choices=['sdf', 'json'],
                        help="Input format. When using STDIN this must be specified.")


def add_default_output_args(parser):
    parser.add_argument('-o', '--output',
                        help="Base name for output file (no extension)."
                             " If not defined then SDTOUT is used for the"
                             " structures and output is used as base name"
                             " of the other files.")
    parser.add_argument('-of', '--outformat', choices=['sdf', 'json'],
                        help="Output format. Defaults to 'sdf'.")
    parser.add_argument('--meta', action='store_true',
                        help='Write metadata and metrics files')


def add_default_io_args(parser):
    add_default_input_args(parser)
    add_default_output_args(parser)


def splitValues(textStr):
    """Splits a comma-separated number sequence into a list (of floats).
    """
    vals = textStr.split(",")
    nums = []
    for v in vals:
        nums.append(float(v))
    return nums


def expandParameters(*args):
    """Expands parameters (presented as tuples of lists and symbolic names)
    so that each is returned in a new list where each contains the same number
    of values.

    Each `arg` is a tuple containing two items: a list of values and a
    symbolic name.
    """
    count = 1
    for arg in args:
        count = max(len(arg[0]), count)
    results = []
    for arg in args:
        results.append(expandValues(arg[0], count, args[1]))
    return tuple(results)


def expandValues(inputs, count, name):
    """Returns the input list with the length of `count`. If the
    list is [1] and the count is 3. [1,1,1] is returned. The list
    must be the count length or 1. Normally called from `expandParameters()`
    where `name` is the symbolic name of the input.
    """
    if len(inputs) == count:
        expanded = inputs
    elif len(inputs) == 1:
        expanded = inputs * count
    else:
        raise ValueError('Incompatible number of values for ' + name)
    return expanded
