import unittest

import os

from pipelines_utils import parameter_utils, utils


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
        json_base = 'test_json'
        json_files = [json_base + '.metadata', json_base + '.data.gz']

        writer = utils.create_simple_writer(json_base, None, 'json', None)

        self.assertTrue(writer is not None)
        # Check files and clean-up
        for json_file in json_files:
            self.assertTrue(os.path.exists(json_file))
            os.remove(json_file)

    def test_create_simple_writer_for_tsv(self):
        """Just check the very basics
        """
        tsv_base = 'test_tsv'
        tsv_files = [tsv_base + '.tsv.gz']

        writer = utils.create_simple_writer('test_tsv', None, 'tsv', None)

        self.assertTrue(writer is not None)
        # Check files and clean-up
        for tsv_file in tsv_files:
            self.assertTrue(os.path.exists(tsv_file))
            os.remove(tsv_file)

    def test_write_metrics(self):
        """Checks metrics are written.
        """
        m_base = 'test_utils'
        m_filename = m_base + '_metrics.txt'

        utils.write_metrics('test_utils', {'key_a': 'value_a',
                                           'key_b': 'value_b'})

        # Read back...
        self.assertTrue(os.path.exists(m_filename))
        metrics = open(m_filename, 'r')
        found_a = False
        found_b = False
        for line in metrics.readlines():
            if line.strip() == 'key_a=value_a':
                found_a = True
            elif line.strip() == 'key_b=value_b':
                found_b = True
        metrics.close()
        os.remove(m_filename)
        self.assertTrue(found_a)
        self.assertTrue(found_b)

    def test_generate_molecule_object_dict_without_values(self):
        """Checks molecule object creation without values.
        """
        source = 'COCC(=O)NC=1C=CC=C(NC(=O)C)C1'
        format = 'smiles'
        values = None

        m = utils.generate_molecule_object_dict(source, format, values)

        self.assertEquals(3, len(m))
        self.assertTrue('uuid' in m)
        self.assertTrue('source' in m)
        self.assertTrue('format' in m)
        self.assertEquals('smiles', m['format'])
        self.assertEquals(source, m['source'])

    def test_generate_molecule_object_dict_with_values(self):
        """Checks molecule object creation with values.
        """
        source = 'COCC(=O)NC=1C=CC=C(NC(=O)C)C1'
        format = 'smiles'
        values = {'a':1}

        m = utils.generate_molecule_object_dict(source, format, values)

        self.assertEquals(4, len(m))
        self.assertTrue('uuid' in m)
        self.assertTrue('source' in m)
        self.assertTrue('format' in m)
        self.assertTrue('values' in m)
        self.assertEquals('smiles', m['format'])
        self.assertEquals(source, m['source'])
        self.assertEquals(values, m['values'])
