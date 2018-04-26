#!/usr/bin/env python
# -*- coding: utf-8 -*-

import unittest

import os

from pipelines_utils import file_utils

# Root of test files,
# relative to test execution directory (src/python)
_TEST_FILE_ROOT = os.path.join('test', 'python2_3', 'pipelines_utils', 'files')


class FileUtilsTestCase(unittest.TestCase):

    def test_pick(self):
        """Test picking a file.
        We expect to find 'test_pick.file'
        """
        self.assertEquals(os.path.join(_TEST_FILE_ROOT, 'test_pick.text'),
                          file_utils.pick('test_pick.text', _TEST_FILE_ROOT))

    def test_pick_unknown_file(self):
        """Test picking a file.
        We expect failure.
        """
        self.assertEquals(None, file_utils.pick('test_unknown.text', _TEST_FILE_ROOT))

    def test_pick_sdf_gz(self):
        """Test picking a file.
        We expect to be given a path to 'test_pick.sdf.gz'
        """
        self.assertEquals(os.path.join(_TEST_FILE_ROOT, 'test_sdf_gz.sdf.gz'),
                          file_utils.pick_sdf('test_sdf_gz', _TEST_FILE_ROOT))

    def test_pick_sdf(self):
        """Test picking a file.
        We expect to be given a path to 'test_pick.sdf'
        """
        self.assertEquals(os.path.join(_TEST_FILE_ROOT, 'test_sdf.sdf'),
                          file_utils.pick_sdf('test_sdf', _TEST_FILE_ROOT))

    def test_pick_unknown_sdf_file(self):
        """Test picking an SDF file.
        We expect failure.
        """
        self.assertEquals(None, file_utils.pick_sdf('test_unknown', _TEST_FILE_ROOT))
