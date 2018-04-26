import os
import unittest

from pipelines_utils import BasicObjectWriter

DATA_DIR = os.path.join('test', 'pipelines_utils', 'data')

class BasicObjectWriterTestCase(unittest.TestCase):

    def test_basic_operation_with_supplied_uuid(self):
        """Test basic file creation.
        """
        filename = 'bow_test_a.tmp'

        bow = BasicObjectWriter.BasicObjectWriter(filename)
        bow.writeHeader()
        bow.write({"A":"a"}, objectUUID="1234567890")
        bow.writeFooter()
        bow.close()

        # Expect something like this: `
        # [{"uuid": "1234567890", "values": {"A": "a"}}]`
        #
        bow_file = open(filename, 'r')
        line = bow_file.readline()
        self.assertTrue(line.startswith('[{'))
        self.assertTrue(line.find('"uuid": "1234567890"') >= 0)
        self.assertTrue(line.find('"values": {"A": "a"}') >= 0)
        self.assertTrue(line.endswith('}]'))
        bow_file.close()
        os.remove(filename)

    def test_basic_operation_with_generated_uuid(self):
        """Test basic file creation.
        """
        filename = 'bow_test_b.tmp'

        bow = BasicObjectWriter.BasicObjectWriter(filename)
        # Chec file
        bow.writeHeader()
        bow.write({"A":"a"})
        bow.writeFooter()
        bow.close()

        # Expect something like this: `
        # [{"uuid": "1234567890", "values": {"A": "a"}}]`
        #
        bow_file = open(filename, 'r')
        line = bow_file.readline()
        self.assertTrue(line.startswith('[{'))
        self.assertTrue(line.find('"uuid": ') >= 0)
        self.assertTrue(line.find('"values": {"A": "a"}') >= 0)
        self.assertTrue(line.endswith('}]'))
        bow_file.close()
        os.remove(filename)
