import unittest

import argparse

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

    def test_expand_parameters_when_unnecessary(self):
        """Verifies expansion of multiple parameters when no expansion is needed.
        """
        params_a = ([1, 1, 1], 'a')
        params_b = ([2, 2, 2], 'b')
        result = parameter_utils.expandParameters(params_a, params_b)
        self.assertEquals(([1, 1, 1], [2, 2, 2]), result)

    def test_expand_parameters_with_real_data(self):
        """Verifies expansion of multiple parameters with some actual values.
        """
        t1 = ([0.135, 0.675, 1.35], 't1')
        t2 = ([0.135, 0.675, 1.35], 't2')
        t3 = ([12.0, 12.0, 12.0], 't3')
        t4 = ([0.5, 0.5, 0.5], 't4')
        t5 = ([7.49, 8.7, 8.7], 't5')
        t6 = ([0.347, 0.17, 0.17], 't6')
        t7 = ([0.29, 0.23, 0.23], 't7')                                                                                                                                                                                                                                               # t_hf_a ([0.5, 0.5, 0.5], 'halflives_abs'
        a, b, c, d, e, f, g = parameter_utils.expandParameters(t1, t2, t3, t4, t5, t6, t7)
        self.assertEquals([0.135, 0.675, 1.35], a)
        self.assertEquals([0.29, 0.23, 0.23], g)

    def test_add_default_input_args_with_short_options(self):
        """Checks ArgParse manipulation.
        """
        parser = argparse.ArgumentParser()

        parameter_utils.add_default_input_args(parser)

        result = parser.parse_args('-i inputfile -if sdf'.split())
        self.assertEquals('inputfile', result.input)
        self.assertEquals('sdf', result.informat)

    def test_add_default_output_args_with_short_options(self):
        """Checks ArgParse manipulation.
        """
        parser = argparse.ArgumentParser()

        parameter_utils.add_default_output_args(parser)

        result = parser.parse_args('-o outputfile -of sdf --meta'.split())
        self.assertEquals('outputfile', result.output)
        self.assertEquals('sdf', result.outformat)
        self.assertTrue(result.meta)

    def test_add_default_input_args_with_long_options(self):
        """Checks ArgParse manipulation.
        """
        parser = argparse.ArgumentParser()

        parameter_utils.add_default_input_args(parser)

        result = parser.parse_args('--input inputfile --informat sdf'.split())
        self.assertEquals('inputfile', result.input)
        self.assertEquals('sdf', result.informat)

    def test_add_default_output_args_with_long_options(self):
        """Checks ArgParse manipulation.
        """
        parser = argparse.ArgumentParser()

        parameter_utils.add_default_output_args(parser)

        result = parser.parse_args('--output outputfile --outformat sdf --meta'.split())
        self.assertEquals('outputfile', result.output)
        self.assertEquals('sdf', result.outformat)
        self.assertTrue(result.meta)
