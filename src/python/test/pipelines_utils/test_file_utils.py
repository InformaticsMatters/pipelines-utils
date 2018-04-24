import unittest
from unittest.mock import patch

import os

from pipelines_utils import file_utils

# Root of test files,
# relative to test execution directory (src/python)
_TEST_FILE_ROOT = 'test/pipelines_utils/files'


class FileUtilsTestCase(unittest.TestCase):

    @patch.dict('os.environ', {file_utils.FILE_ROOT_ENV: _TEST_FILE_ROOT})
    def test_pick(self):
        """Test picking a file.
        We expect to find `test_pick.file`
        """
        self.assertEquals(os.path.join(_TEST_FILE_ROOT,
                                       'test_file_utils',
                                       'test_pick.text'),
                          file_utils.pick('test_pick.text'))

    @patch.dict('os.environ', {file_utils.FILE_ROOT_ENV: _TEST_FILE_ROOT})
    def test_pick_unknown_file(self):
        """Test picking a file.
        We expect failure.
        """
        self.assertEquals(None, file_utils.pick('test_unknown.text'))

    @patch.dict('os.environ', {file_utils.FILE_ROOT_ENV: _TEST_FILE_ROOT})
    def test_pick_sdf(self):
        """Test picking a file.
        We expect to find `test_pick..sdf.gz`
        """
        self.assertEquals(os.path.join(_TEST_FILE_ROOT,
                                       'test_file_utils',
                                       'test_pick.sdf.gz'),
                          file_utils.pick_sdf('test_pick'))

    @patch.dict('os.environ', {file_utils.FILE_ROOT_ENV: _TEST_FILE_ROOT})
    def test_pick_unknown_sdf_file(self):
        """Test picking an SDF file.
        We expect failure.
        """
        self.assertEquals(None, file_utils.pick_sdf('test_unknown'))
