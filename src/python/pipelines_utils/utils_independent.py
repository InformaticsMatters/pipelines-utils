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
import sys, gzip, json, uuid
from math import log10, floor
from rdkit_utils import BasicObjectWriter, TsvWriter # neither of these are RDKit dependent so should be moved

def log(*args, **kwargs):
    """
    Log output to STDERR
    """
    print(*args, file=sys.stderr, **kwargs)

def round_sig(x, sig):
    """Round the number to the specified number of significant figures"""
    return round(x,a sig-int(floor(log10(abs(x))))-1)

def add_default_input_args(parser):
    parser.add_argument('-i', '--input', help="Input file, if not defined the STDIN is used")
    parser.add_argument('-if', '--informat', choices=['sdf', 'json'], help="Input format. When using STDIN this must be specified.")

def add_default_output_args(parser):
    parser.add_argument('-o', '--output', help="Base name for output file (no extension). If not defined then SDTOUT is used for the structures and output is used as base name of the other files.")
    parser.add_argument('-of', '--outformat', choices=['sdf', 'json'], help="Output format. Defaults to 'sdf'.")
    parser.add_argument('--meta', action='store_true', help='Write metadata and metrics files')

def add_default_io_args(parser):
    add_default_input_args(parser)
    add_default_output_args(parser)


def default_open_input_output(inputDef, inputFormat, outputDef, defaultOutput, outputFormat, thinOutput=False, valueClassMappings=None,
                              datasetMetaProps=None, fieldMetaProps=None):
    """Default approach to handling the inputs and outputs"""
    input, suppl = default_open_input(inputDef, inputFormat)
    output,writer,outputBase = default_open_output(outputDef, defaultOutput, outputFormat, thinOutput=thinOutput,
                                                   valueClassMappings=valueClassMappings, datasetMetaProps=datasetMetaProps, fieldMetaProps=fieldMetaProps)
    return input,output,suppl,writer,outputBase


def default_open_input(inputDef, inputFormat):
    if not inputDef and not inputFormat:
        raise ValueError('Must specify either an input file name or an input format (or both)')
    elif inputFormat == 'sdf' or (inputDef and (inputDef.lower().endswith('.sdf') or inputDef.lower().endswith('.sdf.gz'))):
        input, suppl = default_open_input_sdf(inputDef)
    elif inputFormat == 'json' or (inputDef and (inputDef.lower().endswith('.data') or inputDef.lower().endswith('.data.gz'))):
        input, suppl = default_open_input_json(inputDef)
    else:
        raise ValueError('Unsupported input format')

    return input, suppl


def open_file(filename):
    """Open the file gunzipping it if it ends with .gz"""
    if filename.lower().endswith('.gz'):
        return gzip.open(filename)
    else:
        return open(filename, 'r')


def create_simple_writer(outputDef, defaultOutput, outputFormat, fieldNames, compress=True, valueClassMappings=None, datasetMetaProps=None, fieldMetaProps=None):
    """Create a simple writer suitable for writing flat data e.g. as BasicObject or TSV"""

    if not outputDef:
        outputBase = defaultOutput
    else:
        outputBase = outputDef

    if outputFormat == 'json':

        write_squonk_datasetmetadata(outputBase, True, valueClassMappings, datasetMetaProps, fieldMetaProps)

        return BasicObjectWriter(open_output(outputDef, 'data', compress)), outputBase

    elif outputFormat == 'tsv':
        return TsvWriter(open_output(outputDef, 'tsv', compress), fieldNames), outputBase

    else:
        raise ValueError("Unsupported format: " + outputFormat)

def open_output(basename, ext, compress):
    if basename:
        fname = basename + '.' + ext
        if compress:
            fname += ".gz"
            return gzip.open(fname, 'w+')
        else:
            return open(fname, 'w+')
    else:
        if compress:
            # TODO - work out how to write compressed data to STDOUT
            return sys.stdout
        else:
            return sys.stdout

def write_squonk_datasetmetadata(outputBase, thinOutput, valueClassMappings, datasetMetaProps, fieldMetaProps):
    """This is a temp hack to write the minimal metadata that Squonk needs.
    Will needs to be replaced with something that allows something more complete to be written.

    :param outputBase: Base name for the file to write to
    :param thinOutput: Write only new data, not structures. Result type will be BasicObject
    :param valueClasses: A dict that describes the Java class of the value properties (used by Squonk)
    :param datasetMetaProps: A dict with metadata properties that describe the datset as a whole.
            The keys used for these metadata are up to the user, but common ones include source, description, created, history.
    :param fieldMetaProps: A list of dicts with the additional field metadata. Each dict has a key named fieldName whose value
            is the name of the field being described, and a key name values wholes values is a map of metadata properties.
            The keys used for these metadata are up to the user, but common ones include source, description, created, history.
    """
    meta = {}
    props = {}
    # TODO add created property - how to handle date formats?
    if datasetMetaProps:
        props.update(datasetMetaProps)

    if fieldMetaProps:
        meta["fieldMetaProps"] = fieldMetaProps

    if len(props) > 0:
        meta["properties"] = props

    if valueClassMappings:
        meta["valueClassMappings"] = valueClassMappings
    if thinOutput:
        meta['type'] = 'org.squonk.types.BasicObject'
    else:
        meta['type'] = 'org.squonk.types.MoleculeObject'
    s = json.dumps(meta)
    meta = open(outputBase + '.metadata', 'w')
    meta.write(s)
    meta.close()


def write_metrics(baseName, values):
    """Write the metrics data

    :param baseName: The base name of the output files. e.g. extensions will be appended to this base name
    :param values dictionary of values to write
    """
    m = open(baseName  + '_metrics.txt', 'w')
    for key in values:
        m.write(key + '=' + str(values[key]) + "\n")
    m.flush()
    m.close()


def generate_molecule_object_dict(source, format, values):
    """Generate a dictionary that represents a Squonk MoleculeObject when writen as JSON

    :param source: Molecules in molfile or smiles format
    :param format: The format of the molecule. Either 'mol' or 'smiles'
    :param values: Optional dict of values (properties) for the MoleculeObject
    """
    m = {"uuid": str(uuid.uuid4()), "source": source, "format": format}
    if values:
        m["values"] = values
    return m
