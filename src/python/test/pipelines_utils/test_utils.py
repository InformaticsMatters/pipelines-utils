import unittest

from pipelines_utils import utils


class UtilsTestCase(unittest.TestCase):

    def test_round_sig(self):
        """Rounding of significant figures
        """
        self.assertEquals(utils.round_sig(1.23456789, 2), 1.2)
        self.assertEquals(utils.round_sig(1.23456789, 3), 1.23)
        self.assertEquals(utils.round_sig(1.23456789, 6), 1.23457)

        self.assertEquals(utils.round_sig(0.00456789, 3), 0.00457)

        self.assertEquals(utils.round_sig(-1.23456789, 6), -1.23457)

    def test_create_simple_writer_for_json(self):
        """Just check the very basics
        """
        writer = utils.create_simple_writer('experiment', None, 'json', None)
        self.assertTrue(writer is not None)

    def test_create_simple_writer_for_tsv(self):
        """Just check the very basics
        """
        writer = utils.create_simple_writer('experiment', None, 'tsv', None)
        self.assertTrue(writer is not None)
