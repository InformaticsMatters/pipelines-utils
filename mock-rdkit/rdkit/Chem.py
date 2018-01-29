#!/usr/bin/env python

# Mock RDKit.
#
# The very least we can get away with in order to run our own Python unit test.
# The implementation may evolve over time, and even though this module may
# look rather sparse it's all we need to run some of our tests.
#
# Alan Christie
# Jan 2018


def ForwardSDMolSupplier(input):
    pass


def SmilesMolSupplier():
    pass


def MolFromSmarts(line):
    pass


def MolFromMolBlock(text):
    pass


def MolFromSmiles(txt):
    pass


def MolToMolBlock(mol, **kwargs):
    pass


def MolToSmiles(mol, **kwargs):
    pass


def SDWriter(output):
    pass


def Mol():
    pass


class AllChem():

    def Compute2DCoords(self, mol):
        pass
