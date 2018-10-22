import gzip
import os
import unittest

from pipelines_utils import TypedColumnReader

DATA_DIR = os.path.join('test', 'python2_3', 'pipelines_utils', 'data')


class TypedColumnReaderTestCase(unittest.TestCase):

    def test_basic_example_a(self):
        """Test loading of a simple CSV file
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.a.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file, column_sep=',')
        num_lines = 0
        first_row = {}
        for row in test_file:
            if num_lines == 0:
                first_row = row
            num_lines += 1
        self.assertEqual(2, num_lines)
        self.assertEqual('A string', first_row['one'])
        self.assertEqual('and finally', first_row['four'])
        csv_file.close()

    def test_basic_example_a_with_supplied_header(self):
        """Test loading of a simple CSV file with a provided header
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.a-no-header.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file,
                                                        column_sep=',',
                                                        header='one,two:int,three:float,four:string')
        num_lines = 0
        for _ in test_file:
            num_lines += 1
        self.assertEqual(2, num_lines)
        csv_file.close()

    def test_basic_example_a_gzip(self):
        """Test loading of a simple CSV file (gzipped)
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.a.csv.gz')
        csv_file = gzip.open(test_file, 'rt')
        test_file = TypedColumnReader.TypedColumnReader(csv_file, column_sep=',')
        num_lines = 0
        for _ in test_file:
            num_lines += 1
        self.assertEqual(2, num_lines)
        csv_file.close()

    def test_basic_example_b_unknown_type(self):
        """Test loading of a simple CSV file with a column type that is unknown
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.b.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file, column_sep=',')
        num_lines = 0
        got_exception = False
        try:
            for _ in test_file:
                num_lines += 1
        except TypedColumnReader.UnknownTypeError as e:
            self.assertEqual(4, e.column)
            self.assertAlmostEqual('unknown-type', e.column_type)
            got_exception = True
        self.assertTrue(got_exception)
        self.assertEqual(0, num_lines)
        csv_file.close()

    def test_basic_example_c_too_many_colons(self):
        """Test loading of a simple CSV file with a column that has too many colons
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.c.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file, column_sep=',')
        num_lines = 0
        got_exception = False
        try:
            for _ in test_file:
                num_lines += 1
        except TypedColumnReader.ContentError as e:
            self.assertEqual(4, e.column)
            self.assertEqual(1, e.row)
            self.assertAlmostEqual('four:unknown-type:too-many-colons', e.value)
            got_exception = True
        self.assertTrue(got_exception)
        self.assertEqual(0, num_lines)
        csv_file.close()

    def test_basic_example_d_wrong_type(self):
        """Test loading of a simple CSV file with a column that has a string as an int
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.d.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file, column_sep=',')
        num_lines = 0
        got_exception = False
        try:
            for _ in test_file:
                num_lines += 1
        except TypedColumnReader.ContentError as e:
            self.assertEqual(1, e.column)
            self.assertEqual(2, e.row)
            self.assertAlmostEqual('A string', e.value)
            self.assertAlmostEqual('Does not comply with column type', e.message)
            got_exception = True
        self.assertTrue(got_exception)
        self.assertEqual(0, num_lines)
        csv_file.close()

    def test_basic_example_d_tabs(self):
        """Test loading of a simple CSV file with tab (default) separators
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.e.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file)
        num_lines = 0
        for _ in test_file:
            num_lines += 1
        self.assertEqual(2, num_lines)
        csv_file.close()

    def test_basic_example_d_too_many_values(self):
        """Test loading of a simple CSV file with too many values
        """
        test_file = os.path.join(DATA_DIR, 'TypedCsvReader.example.f.csv')
        csv_file = open(test_file)
        test_file = TypedColumnReader.TypedColumnReader(csv_file, column_sep=',')
        num_lines = 0
        got_exception = False
        try:
            for _ in test_file:
                num_lines += 1
        except TypedColumnReader.ContentError as e:
            self.assertEqual(3, e.column)
            self.assertEqual(2, e.row)
            self.assertAlmostEqual('Too many values', e.message)
            got_exception = True
        self.assertTrue(got_exception)
        self.assertEqual(0, num_lines)
        csv_file.close()
