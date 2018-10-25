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

"""Typed column (CSV) reader.

Based on the built-in ``csv`` module, this Generator module provides the user
with the ability to load _typed_ CSV-like content, a text file of values
that include a header with optional type specifications provided in the
header.

Alan Christie
October 2018
"""

import csv


class Error(Exception):
    """Base class for exceptions in this module."""
    pass


class UnknownTypeError(Error):
    """Exception raised for an unknown type in the header.

    Attributes:
        column -- the problematic (1-based) column number
        column_type -- The column's type value
    """

    def __init__(self, column, column_type):
        self.column = column
        self.column_type = column_type


class ContentError(Error):
    """Exception raised for CSV content errors.
    This is raised if the column value is unknown or does not
    comply with the defined type.

    Attributes:
        column -- the problematic (1-based) column number
        row -- the problematic (1-based) row number
        value -- The value (or None if n/a)
        message -- explanation of the error
    """

    def __init__(self, column, row, value, message):
        self.column = column
        self.row = row
        self.value = value
        self.message = message


def convert_int(string_value):
    """Converts a string to integer (see CONVERTERS).
    There is a converter function for each column type.

    :param string_value: The string to convert

    :raises: ValueError if the string cannot be represented by an int
    """
    return int(string_value.strip())


def convert_float(string_value):
    """Converts string to float (see CONVERTERS).
    There is a converter function for each column type.

    :param string_value: The string to convert

    :raises: ValueError if the string cannot be represented by a float
    """
    return float(string_value.strip())


def convert_string(string_value):
    """String and default converter (see CONVERTERS).
    There is a converter function for each column type.

    :param string_value: The string to convert
    """
    return string_value


# A map of column type names (case-insensitive) to string conversion function.
# If a column is 'name:INT' then we call 'convert_int()' for the column values.
CONVERTERS = {'int': convert_int,
              'float': convert_float,
              'string': convert_string}


class TypedColumnReader(object):
    """A generator to handle 'typed' CSV-like files, files that include
    a header that may also define data types. This class supports
    neo4j-like node/edge column typing where fields are annotated
    with type information where each column header is of the format
    ``name[:type]``.

    As a Generator it returns a dictionary of values for each row in the file
    where, if the column header defines a type, the value is converted to that
    type.

    There is built-in support for ``int``, ``float`` and ``string`` data types.

    As an example, the following is a comma-separated header for a file with
    columns ``names`` "smiles", "comment", "hac" and "ratio" where
    the first two column types are strings and the last two are
    ``int`` and ``float`` types: -

    "smiles,comment:string,hac:int,ratio:float"

    *   The ``name`` cannot be blank and must be unique.
    *   Whitespace is stripped from the start and end of the column ``name``
    *   If a column value is empty/blank the corresponding dictionary
        value is ``None``


    """

    def __init__(self, csv_file,
                 column_sep='\t',
                 type_sep=':',
                 header=None):
        """Basic initialiser.

        :param csv_file: The typed CSV-like file. csv_file can be any object
                         tha supports the iterator protocol and returns a string
                         each time its next() method is called
        :param column_sep: The file column separator
        :param type_sep: The type separator, the character between the column
                         header name and its type declaration.
        :param header: An optional header. This is provided to allow processing
                       of existing files that have no header. The header
                       must contain column names.
                       "smiles:string" would be a column named "smiles"
                       of type "string" and "n:int" would be a column known as
                       "n" of type "integer". When provided here the header
                       column separator must be comma-separated.
        """

        self._csv_file = csv_file
        self._type_sep = type_sep
        self._header = header

        self._c_reader = csv.reader(self._csv_file,
                                    delimiter=column_sep,
                                    skipinitialspace=True,
                                    strict=True)

        # Column value type converter functions.
        # An entry for each column in the file and compiled by _handle_header()
        # using the provided header or file content on the first iteration.
        self._converters = []
        # The ordered list of unique column names extracted from the header
        self._column_names = []

    def __iter__(self):
        """Return the next type-converted row from the file.
        Unless the header is provided in the initialiser, the first row is
        expected to be a header with optional type declarations.

        If the column value is empty/blank the corresponding dictionary
        value is None.

        :returns: A dictionary of type-converted values for the next row
                  where the dictionary key is the name of the column
                  (as defined in the header).

        :raises: ValueError if a column value cannot be converted
        :raises: ContentError if the column value is unknown or does not
                              comply with the column type.
        :raises: UnknownTypeError if the column type is unknown.
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
            # If we were given a header during initialisation
            # then there's no header in the file
            if not self._converters:
                self._handle_hdr(row)
                continue

            # Must have a header if we get here...
            if len(self._converters) == 0:
                raise ContentError(1, 1, None, 'Missing header')

            # Construct a dictionary of row column names and values,
            # applying type conversions based on the
            # type defined in the header
            row_content = {}
            col_index = 0
            for col in row:
                # Too many items in the row?
                # Can't have a header with 4 columns and a file of 5
                if col_index >= len(self._converters):
                    raise ContentError(col_index + 1, self._c_reader.line_num,
                                       None, 'Too many values')
                lean_col = col.strip()
                col_val = None
                if lean_col:
                    try:
                        col_val = self._converters[col_index][1](col)
                    except ValueError:
                        raise ContentError(col_index + 1, self._c_reader.line_num,
                                           col,
                                           'Does not comply with column type')
                row_content[self._column_names[col_index]] = col_val
                col_index += 1

            yield row_content

    def _handle_hdr(self, hdr):
        """Given the file header line (or one provided when the object
        is instantiated) this method populates the ``self._converters`` array,
        a list of type converters indexed by the column name.

        :param hdr: The header line.

        :raises: ContentError for any formatting problems
        :raises: UnknownTypeError if the type is not known
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
            if name in self._column_names:
                raise ContentError(column_number, self._c_reader.line_num,
                                   name, 'Duplicate column name')

            if len(cell_parts) == 2:
                column_type = cell_parts[1].strip().lower()
                if column_type not in CONVERTERS:
                    raise UnknownTypeError(column_number, column_type)
            else:
                # Unspecified - assume built-in 'string'
                column_type = 'string'
            self._converters.append([name, CONVERTERS[column_type]])
            self._column_names.append(name)
            column_number += 1
