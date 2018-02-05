import os
import unittest

from rdkit_utils import StreamJsonListLoader

DATA_DIR = os.path.join('test', 'rdkit_utils', 'data')

class StreamJsonListLoaderTestCase(unittest.TestCase):

    def test_basic_operation(self):
        """Test loading of a simple JSON list file
        """
        test_file = os.path.join(DATA_DIR, 'StreamJsonListLoader.example.json')
        loader = StreamJsonListLoader.StreamJsonListLoader(test_file)
        # Load the JSON list (one at a time)
        entry = next(loader)
        self.assertEquals('11', entry['a'])
        self.assertEquals('12', entry['b'])
        entry = next(loader)
        self.assertEquals('21', entry['a'])
        self.assertEquals('22', entry['b'])
        entry = next(loader)
        self.assertEquals('31', entry['a'])
        self.assertEquals('32', entry['b'])
        got_end_of_data = False
        try:
            next(loader)
        except StopIteration:
            got_end_of_data = True
        self.assertTrue(got_end_of_data)
        loader.close()
