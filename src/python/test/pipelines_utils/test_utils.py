import unittest

from pipelines_utils import utils


class UtilsTestCase(unittest.TestCase):

    def test_round_sig(self):
        """Rounding of significant figures
        """
        self.assertEquals(utils.round_sig(1.23456789, 2), 1.2)
        self.assertEquals(utils.round_sig(1.23456789, 3), 1.23)
        self.assertEquals(utils.round_sig(1.23456789, 6), 1.23457)

        self.assertEquals(utils.round_sig(-1.23456789, 6), -1.23457)
