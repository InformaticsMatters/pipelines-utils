import os
import unittest

from pipelines_utils import StreamJsonListLoader

DATA_DIR = os.path.join('test', 'python2_3', 'pipelines_utils', 'data')


class StreamJsonListLoaderTestCase(unittest.TestCase):

    def test_basic_operation_with_next(self):
        """Test loading of a simple JSON list file
        """
        test_file = os.path.join(DATA_DIR, 'StreamJsonListLoader.example.json')
        loader = StreamJsonListLoader.StreamJsonListLoader(test_file)
        # Load the JSON list (one at a time)
        entry = next(loader)
        self.assertEqual('11', entry['a'])
        self.assertEqual('12', entry['b'])
        entry = next(loader)
        self.assertEqual('21', entry['a'])
        self.assertEqual('22', entry['b'])
        entry = next(loader)
        self.assertEqual('31', entry['a'])
        self.assertEqual('32', entry['b'])
        got_end_of_data = False
        try:
            next(loader)
        except StopIteration:
            got_end_of_data = True
        self.assertTrue(got_end_of_data)
        loader.close()

    def test_basic_operation_with_for(self):
        """Test loading of a simple JSON list file
        """
        test_file = os.path.join(DATA_DIR, 'StreamJsonListLoader.example.json')
        loader = StreamJsonListLoader.StreamJsonListLoader(test_file)
        # Load the JSON list (one at a time)
        num_entries = 0
        for entry in loader:
            num_entries += 1
        self.assertEqual(3, num_entries)
        loader.close()

    def test_bad_1_file(self):
        """Test loading of a simple JSON file (that's not a list)
        """
        test_file = os.path.join(DATA_DIR, 'StreamJsonListLoader.bad.1.json')
        got_exception = False
        try:
            StreamJsonListLoader.StreamJsonListLoader(test_file)
        except NotImplementedError as e:
            self.assertTrue(str(e).startswith('Only JSON-streams of lists'))
            got_exception = True
        self.assertTrue(got_exception)
