#!/usr/bin/env python

# Setup module for themMock RDKit Pipelines Utilities.
#
# Jan 2018

from setuptools import setup, find_packages

setup(

    name='im-mock-rdkit',
    version='0.1.0',
    author='Alan Christie',
    author_email='alan.christie@informaticsmatters.com',
    url='https://github.com/InformaticsMatters/pipelines-utils',
    license='Copyright (C) 2018 Informatics Matters Ltd. All rights reserved.',
    description='A simple mock of RDKit for Informatics Matters Pipelines testing',
    long_description='A mock-up of RDKit to permit pipelines unit testing.',
    keywords='rdkit,pipelines',
    platforms=['any'],

    # Our modules to package
    packages=find_packages(exclude=["*.test", "*.test.*", "test.*", "test"]),

    # Project classification:
    # https://pypi.python.org/pypi?%3Aaction=list_classifiers
    classifiers=[
        'Development Status :: 5 - Production/Stable',
        'Environment :: Console',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3.6',
        'Topic :: Software Development :: Build Tools',
    ],

    install_requires=[],

    zip_safe=False,

)
