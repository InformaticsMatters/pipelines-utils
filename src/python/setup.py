#!/usr/bin/env python

# Setup module for the Python-based Pipelines Utilities.
#
# Jan 2018

from setuptools import setup, find_packages

def get_long_description():
    return open('README.rst').read()

setup(

    name='im-pipelines-utils',
    version='1.0.0.rc1',
    author='Alan Christie',
    author_email='achristie@informaticsmatters.com',
    url='https://github.com/InformaticsMatters/pipelines-utils',
    license='Copyright (C) 2018 Informatics Matters Ltd. All rights reserved.',
    description='Utilities for Informatics Matters Pipelines',
    long_description=get_long_description(),
    keywords='pipelines',
    platforms=['any'],

    # Our modules to package
    packages=find_packages(exclude=['*.test', '*.test.*', 'test.*', 'test']),

    # Essential dependencies
    install_requires=[
        'future >= 0.16.0'
    ],
    # Supported Python versions
    # 2.7 and 3.5 or better
    python_requires='>=2.7, !=3.0.*, !=3.1.*, !=3.2.*, !=3.3.*, != 3.4.*, <4',

    # Project classification:
    # https://pypi.python.org/pypi?%3Aaction=list_classifiers
    classifiers=[
        'Development Status :: 5 - Production/Stable',
        'Environment :: Console',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
        'Topic :: Software Development :: Build Tools',
    ],

    # Root of the test suite
    test_suite = 'test',

    zip_safe=False,

)
