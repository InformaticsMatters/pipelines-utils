[![Build Status](https://travis-ci.org/InformaticsMatters/pipelines-utils.svg?branch=master)](https://travis-ci.org/InformaticsMatters/pipelines-utils)
[![Coverage Status](https://coveralls.io/repos/github/InformaticsMatters/pipelines-utils/badge.svg?branch=master)](https://coveralls.io/github/InformaticsMatters/pipelines-utils?branch=master)

# Pipelines Utils
A repository of common **Informatics Matters** _Pipeline_ utilities shared
between a number of _Pipelines_ repositories. As well as containing a number
of modules previously resident in the [Pipelines] repository this repository
contains a [Groovy]-based test framework that interprets a relatively simple
text-file format used to both describe and execute built-in tests.

## Execution environment
You will need: -

-   Java
-   Conda
-   Groovy (v2.4)
-   Python

>   It is vital that you install and setup a Java installation (especially by
    also setting `JAVA_HOME` correctly) _before_ you install groovy. Observed
    on Windows 7. If you do not groovy assumes a 32-bit environment and
    then cannot call into a 64-bit java. 

Although the project is based on [Gradle], which is Groovy-based,
you will need to install **Groovy**. We've tested this framework using Groovy
`v2.4.11`.

The pipelines are based on the [RDKit] Open-Source Cheminformatics Software
and are best executed from within a suitably configured [Conda] environment.
You may need to install some additional modules before you can run the tests,
these dependencies are captured in our own `requiremenmts.txt` file and
installed as normal:

    $ pip install -r requiremenmts.txt

>   The module utilities should support both Python 2 and 3 but we recommend
    any modules/pipelines you write support both flavours.

## Running the test framework
>   We plan to release the Python utility modules to [PIP] at some stage. The
    distribution is based on `setup.py` is present and works and will be
    released when more invasive tests have been written. In the meantime we
    use this repository as a Git [submodule] in our existing pipelines.

### Redirecting output
Normally pipeline output files are written to a `tmp` directory inside
the working copy of the repository you're running in. Alternatively you
can write test output to your own directory (i.e. `/tmp/blob`) using
the environment variable `POUT`: -

    $ export POUT=/tmp/blob/

Output files are removed when the test suite starts and when it passes.

### From here
If you have working copies of all your pipeline repositories checked-out
in the same directory as this repository you can execute all the tests
across all the repositories by running the tester from here. Simply
run the following Gradle command from here: -

    $ ./gradlew runPipelineTester

The tester summarises each test it has found wile also executing it.
When tests fail it logs as much as it can and continues. When all the tests
have finished it prints a summary including a lit of failed tests along with
of the number of test files and individual tests that were executed: -

    +----------------+
    |         Summary| 
    +----------------+
    |      Test files|  29
    |     Tests found|  39
    |    Tests passed|  20
    |    Tests failed|   -
    |   Tests skipped|  19
    |   Tests ignored|   3
    |        Warnings|   -
    +----------------+
    |          Result| SUCCESS
    +----------------+

Fields in the summary should be self-explanatory but a couple might benefit
from further explanation: -

-   `Tests skipped` are the tests that were found but not executed.
    Normally these are the tests found during a _Container_ test
    that can't be run (because the test has no corresponding container image).
-   `Tests ignored` are test found that are not run because they have
    been marked for non-execution as the test name begins with `ignore_`. 
    
### Limiting test search
You can limit the tests that are located by defining the parent
directories that you want executed on the command-line. From gradle
you can add the test directories as a comma-separated list with the `-o`
option. To only run the tests in the pipelines project your command-line
would look like this: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines

Additionally, if you only want to run some specific tests within
a directory you can added their names to the directory with `.`.
For example, to only run the test `test_x_100` in the `pipelines`
directory: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines.test_x_100

To run all the tests in the `pipelines` directory and the `test_probe`
test in the `pipelines-2` directory: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines,pipelines-2.test_probe

### In Docker
You can run the pipeline tests in Docker using their expected container
image (defined in the service descriptor). Doing this gives you added
confidence that your pipeline will work wen deployed.

You can use the docker-specific Gradle task: -

    $ ./gradlew runDockerPipelineTester
    
Or, by adding the `-d` or `--indocker` command-line argument into the basic
task. To pass command-line options through Gradle into the underlying task
you can also run the Docker tests like this: -

    $ ./gradlew runPipelineTester -Pptargs=-d

>   When you run _in docker_ only the tests that can run in Docker (those with
    a defined image name) will be executed. Tests that cannot be executed in
    Docker will be _skipped_.
    
## Debugging test failures
Ideally your tests will pass. When they don't the test framework prints
the collected log to the screen as it happens but also keeps all the files
generated (by all the tests) in case they are of use for diagnostics.

Files generated by the executed pipelines are temporarily written
to the `src/groovy/tmp/PipelineTester` directory. You will find a directory
for every file and test combination. For example, if the test file 
`pbf_ev.test` contains the test `test_pbf_ev_raw` any files it generates
will be found in the directory `pbf_ev-test_pbf_ev_raw`.

You can re-direct the test output to an existing directory of your choice by
defining the Environment variable `POUT`: -

    $ export POUT=/tmp/my-output-dir/
    
Some important notes: -

-   Files generated by the pipelines are removed when the tester is
    re-executed and is all the tests pass
-   Generated files are not deleted until the test framework has finished.
    If your tests generate large output files you may need to make sure your
    disk can accommodate all the output from all the tests
-   When you run the pipeline tester it removes any remaining collected
    output files 

## Writing pipeline tests
The `PipelineTester` looks for files that have the extension `.test` that
can be found in your pipelines project. Tests need to be placed in the
directory that contains the pipeline they're testing.

As well as copying existing test files you can use the `pipeline.test.template`
file, in this project's root, as a documented template file
in order to create a set of tests for a new pipeline.

>   At the moment the tester only permits one test file per pipeline so all 
    tests for a given pipeline need to be composed in one file. 

## Testing the pipeline utilities
The pipeline utilities consist of a number of Python-based modules
that can be tested using `setup.py`. To test these modules run the
following from the `src/python` directory: -

    $ python setup.py test

## Considerations for Windows
The tests and test framework are designed to operate from within a unix-like
environment but if you are forced to execute pipeline tests from Windows the
following approach is recommended: -

You will need:

-   Java 8
-   Conda
-   Groovy (v2.4)
-   Git-Bash

1.  Install Java 8 (ideally the JDK) and define [JAVA_HOME]. This **MUST**
    be done _before_ installing Groovy.
1.  Install [Git for Windows]. This will give you a unix bash-like
    execution environment
1.  From within the Git-bash shell navigate to your pipelines project.
1.  Ensure that you can execute both Python and Groovy from within the
    Git-Bash shell (i.e. `python --version` and `groovy --version` work)
1.  From the pipelines project root enter your Conda environment
    with something like `source activate my-conda-env`. To run the
    pipelines tests your environment must contain the rdkit package. It
    can be installed with this command from within Conda...
    
    $ conda install -c rdkit rdkit
    
1.  Install additional modules required by `pipelines-utils` but
    using its requirements file (which can be found in the `pipelines-utils`
    sub-project): -
    
    $ pip install -r requirements.txt

With the above steps complete you should be able to execute the pipelines
tester by navigating to the sub-module in your pipelines project: -

    $ cd pipelines-utils
    $ ./gradlew runPipelineTester

## Publishing im-pipelines-utils to PyPI
The `src/python` directory contains common Python-based pipelines utilities
used by other pipelines repositories. These utilities are published to PyPI
for easy installation (normally automatically by the Travis CI/CD framework
when the repository is tagged on master).

If you are going to publish the utilties yourself (not recommended) you will
need an account on PyPI. For Informatics Matters you should add the
following to your `~/pypirc` file (or create one if you don't have one): -

    [pypi]
    username: informaticsmatters
    password: <password>


To publish a new set of Python utilities you then simply need to build
and upload them from the `src/python` directory: -

    $ pip install -r requirments.txt
    $ python setup.py bdist_wheel
    $ twine upload dist/*

---

[Conda]: https://conda.io/docs/
[Git for Windows]: http://gitforwindows.org
[Gradle]: https://gradle.org
[Groovy]: http://groovy-lang.org
[JAVA_HOME]: https://docs.oracle.com/cd/E19182-01/820-7851/inst_cli_jdk_javahome_t/
[PIP]: https://pypi.python.org/pypi
[Pipelines]: https://github.com/InformaticsMatters/pipelines.git
[PyPI]: https://pypi.python.org/pypi
[RDKit]: http://www.rdkit.org
[Submodule]: https://git-scm.com/docs/gitsubmodules
