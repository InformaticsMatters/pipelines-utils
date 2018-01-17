import unittest

from pipelines_utils import parameter_utils


class ParameterUtilsTestCase(unittest.TestCase):

    def test_split_values(self):
        """Verifies basic feature.
        """
        result = parameter_utils.splitValues('1,2,3')
        self.assertEqual([1.0, 2.0, 3.0], result)

    def test_expand_values_1_to_3(self):
        """Verifies basic feature.
        """
        result = parameter_utils.expandValues([1], 3, 'blob')
        self.assertEqual([1, 1, 1], result)

    def test_expand_values_3(self):
        """Verifies basic feature.
        """
        result = parameter_utils.expandValues([1, 1, 1], 3, 'blob')
        self.assertEqual([1, 1, 1], result)

    def test_expand_values_value_error(self):
        """Verifies handling of odd inputs.
        """
        got_excpetion = False
        try:
            parameter_utils.expandValues([1, 1], 3, 'blob')
        except ValueError as e:
            self.assertTrue('Inc ompatible number of values for blob', e)
            got_excpetion = True
        self.assertTrue(got_excpetion)

    def test_expand_parameters(self):
        """Verifies expansion of multiple parameters.
        """
        params_a = ([1], 'a')
        params_b = ([2, 2, 2], 'b')
        result = parameter_utils.expandParameters(params_a, params_b)
        self.assertEquals(([1, 1, 1], [2, 2, 2]), result)
