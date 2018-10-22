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
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Typed CSV reader.

Based on the built-in ``csv`` module, this Generator module provides the user
with the ability to load _typed_ CSV content, a CSV file with optional type
specifications provided in the header (which must be supplied).

Alan Christie
October 2018
"""

import csv
import gzip


class Error(Exception):
    """Base class for exceptions in this module."""
    pass


class UnknownTypeError(Error):
    """Exception raised for an unknown type in the header.

    Attributes:
        column -- the problematic column number
        column_type -- The column type
    """

    def __init__(self, column, column_type):
        self.column = column
        self.column_type = column_type


class ContentError(Error):
    """Exception raised for CSV content errors.
    This is raised if the column value is unknown or does not
    comply with the defined type.

    Attributes:
        column -- the problematic column number
        row -- the problematic (1-based) row number
        value -- The value (or None is n/a)
        message -- explanation of the error
    """

    def __init__(self, column, row, value, message):
        self.column = column
        self.row = row
        self.value = value
        self.message = message


def convert_int(string_value):
    """Converts string to integer (see CONVERTERS).
    There is a ``converter_?()`` function for each column type.

    :param string_value: The string to convert
    """
    return int(string_value.strip())


def convert_float(string_value):
    """Converts string to float (see CONVERTERS).
    There is a ``converter_?()`` function for each column type.

    :param string_value: The string to convert
    """
    return float(string_value.strip())


def convert_string(string_value):
    """String and default converter (see CONVERTERS).
    There is a ``converter_?()`` function for each column type.

    :param string_value: The string to convert
    """
    return string_value


# A map of built-in column type to string conversion function.
# If a column is 'name:INT' then we call 'convert_int()'
# for the column values.
CONVERTERS = {'int': convert_int,
              'float': convert_float,
              'string': convert_string}


class TypedColumnReader(object):

    """A generator to handle 'typed' CSV-like files, files (normally)
    with a header that can include type information. This class supports
    neo4j-like column typing where field are annotated
    with type information. The class returns
    a list of values for each row in the file where, if the column header
    defines a type, the value is converted to that type.

    There is built-in support for ``int``, ``float`` and ``string`` data types.

    The following is a comma-separated header for a file where the first two
    columns contain strings and the last two contain `int`` and ``float``
    types: -

        "smiles,comment:string,hac:int,ratio:float"
    """

    def __init__(self, filename,
                 column_sep='\t',
                 type_sep=':',
                 header=None):
        """Basic initialiser.

        :param filename: The typed CSV file name
        :param column_sep: The file column separator
        :param type_sep: The type separator
        :param header: An optional header. If provided the must not have
                       a header line. This is provided to allow processing
                       of exiting files that have no header. You are strongly
                       encouraged to create new files with a header.
        """

        self._filename = filename
        self._type_sep = type_sep
        self._header = header

        # Open the CSV file (which may be compressed)
        if filename.endswith('.gz'):
            self._csv_file = gzip.open(filename, 'rt')
        else:
            self._csv_file = open(filename, 'rt')
        self._c_reader = csv.reader(self._csv_file,
                                    delimiter=column_sep,
                                    skipinitialspace=True,
                                    strict=True)

        # Column value type converter functions.
        # An entry for each column in the file and compiled by _handle_header
        # using the provided header or file content oin the first iteration.
        self._converters = []
        # The the column names extracted from the header
        self._column_names = []

    def __iter__(self):
        """Return the next type-converted row from the file.
        The first row is expected to be a header with optional
        type definitions.

        :returns: A dictionary of type-converted values for the next row

        :raises: ValueError if a column value cannot be converted
        :raises: ContentError if the column value is unknown or does not
                              comply with the column type.
        """

        # If we have not generated the converter array but we have been given
        # a header then now's the time to build the list of type converters.
        # A specified header is always comma-separated, regardless of
        # the separator used in the file.
        if not self._converters and self._header:
            self._handle_hdr(self._header.split(','))

        for row in self._c_reader:

            # Handle the first row?
            # (which defines column names and types)
            if not self._converters:
                self._handle_hdr(row)
                continue

            # Must have seen a header if we get here...
            if len(self._converters) == 0:
                raise ContentError(1, 1, None, 'Missing header')

            # Construct a dictionary of row column names and values,
            # applying type conversions based on the
            # type defined in the header....
            row_content = {}
            col_index = 0
            # Convert...
            for col in row:
                # Too many items in the row?
                if col_index >= len(self._converters):
                    raise ContentError(col_index + 1, self._c_reader.line_num,
                                       None, 'Too many values')
                try:
                    row_content[self._column_names[col_index]] =\
                        self._converters[col_index][1](col)
                except ValueError:
                    raise ContentError(col_index + 1, self._c_reader.line_num,
                                       col,
                                       'Does not comply with column type')
                col_index += 1

            yield row_content

    def _handle_hdr(self, hdr):
        """Given the file header line (or one provided when the class
        is instantiated) this method populates the self.converters array,
        a list of type converters indexed by column.

        :param hdr: The header line.
        """

        column_number = 1
        for cell in hdr:
            cell_parts = cell.split(self._type_sep)
            if len(cell_parts) not in [1, 2]:
                raise ContentError(column_number, self._c_reader.line_num,
                                   cell, 'Expected name and type (up to 2 items)')
            name = cell_parts[0].strip()
            if len(name) == 0:
                raise ContentError(column_number, self._c_reader.line_num,
                                   cell, 'Column name is empty')
            if len(cell_parts) == 2:
                column_type = cell_parts[1].lower()
                if column_type not in CONVERTERS:
                    raise UnknownTypeError(column_number, column_type)
            else:
                # Unspecified - assume built-in 'string'
                column_type = 'string'
            self._converters.append([name, CONVERTERS[column_type]])
            self._column_names.append(name)
            column_number += 1

    def __del__(self):
        """Delete method.
        """
        if self._csv_file:
            self._csv_file.close()
            self._csv_file = None
