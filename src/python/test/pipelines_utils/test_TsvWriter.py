import os
import unittest

from collections import OrderedDict

from pipelines_utils import TsvWriter

DATA_DIR = os.path.join('test', 'pipelines_utils', 'data')

class TsvWriterTestCase(unittest.TestCase):

    def test_basic_operation_with_supplied_uuid(self):
        """Test basic file creation.
        """
        filename = 'bow_test_a.tmp'
        header = OrderedDict()
        header['a'] = 'one'
        header['b'] = 'two'

        tw = TsvWriter.TsvWriter(filename, header)
        tw.writeHeader()
        tw.write({'a': '123', 'b': '456'})
        tw.writeFooter()
        tw.close()

        # Expect something like this: `
        # [{"uuid": "1234567890", "values": {"A": "a"}}]`
        #
        tw_file = open(filename, 'r')
        line = tw_file.readline()
        self.assertEquals('one\ttwo\n', line)
        line = tw_file.readline()
        self.assertEquals('123\t456\n', line)
        tw_file.close()
        os.remove(filename)
