# Pipelines Utils

![build](https://github.com/InformaticsMatters/pipelines-utils/workflows/build/badge.svg)
![publish](https://github.com/InformaticsMatters/pipelines-utils/workflows/publish/badge.svg)

[![PyPI version](https://badge.fury.io/py/im-pipelines-utils.svg)](https://badge.fury.io/py/im-pipelines-utils)

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

>   The module utilities should support both Python 2 and 3 but we recommend
    any modules/pipelines you write support both flavours.

## Running the test framework

### Redirecting input (PIN)
The test utility sets up the `PIN` environment variable, which is set
to the data directory within your pipelines project
(`<project-root>/src/data`). All input date files that you want to use for
your testing must reside in this directory.

### Redirecting output (POUT)
Normally pipeline output files are written to a `tmp` directory inside
the working copy of the `pipelines-utils` repository you're running in.
Alternatively you can write test output to your own directory
(i.e. `/tmp/blob`) using the environment variable `POUT`: -

    $ export POUT=/tmp/blob/

>   Output files are removed when the test suite starts and when it passes.

### The Program Root (PROOT)
Some tests rely on the definition of the `PROOT` environment variable.
If `PROOT` is used it will have been defined in the the Dockerfile that
accompanies the pipelines project.

For shell (outside Docker) execution the test utility defines this for you.
`PROOT` is set to the execution directory, typically
`<project-root>/src/python`. If you have pipelines-specific files (Pickle
files and the like) tey should be located relative to this directory.

### From here
If you have working copies of all your pipeline repositories checked-out
in the same directory as this repository you can execute all the tests
across all the repositories by running the tester from here. Simply
run the following Gradle command from te root of the `pipelines-utils`
project: -

    $ ./gradlew runPipelineTester

The tester summarises each test it has found wile also executing it.
When tests fail it logs as much as it can and continues. When all the tests
have finished it prints a summary including a lit of failed tests along with
of the number of test files and individual tests that were executed: -

    +------------------+
    |           Summary| 
    +------------------+
    |    Test directory| pipelines-utils
    |    Test directory| pipelines
    +------------------+
    |        Test files|  29
    |       Tests found|  39
    |      Tests passed|  20
    |      Tests failed|   -
    |     Tests skipped|  19
    |     Tests ignored|   3
    |    Tests excluded|   -
    +------------------+
    |            Result| SUCCESS
    +------------------+

Fields in the summary should be self-explanatory but a couple might benefit
from further explanation: -

-   `Tests skipped` are the tests that were found but not executed.
    Normally these are the tests found during a _Container_ test
    that can't be run (because the test has no corresponding container image).
-   `Tests ignored` are tests found that are not run because they have
    been marked for non-execution as the test name begins with `ignore_`. 
-   `Tests excluded` are tests found that are not run because the
    operating system matches a regular expression in the test's
    `exlcude` declaration. 
    
### Limiting test search
You can limit the tests that are executed by defining the parent
directories that you want the tester to explore via the command-line.

>   For simple single argument execution you can run form within gradle.
    For fine control and multiple arguments you're better-off navigating
    to the PipelineTester implementation (`src/groovy`) and running it
    manually: `groovy PipelineTester.groovy -s -o pipelines-sygnature`.
 
From gradle you can add the test directories as a comma-separated list
with the `-o` option. To only run the tests in the `pipelines` project your
command-line would look like this: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines

Additionally, if you want to limit test execution further by naming
specific tests within a project you can added their names to the directory
with `.`. For example, to only run the test `test_x_100` in the `pipelines`
project: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines.test_x_100

You can add more tests if you want by adding further comma-separated values.
To add `test_v` in the `pipleines_demo` project for example: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines.test_x_100,pipelines-demo.test_v

To run all the tests in the `pipelines` directory and the `test_probe`
test in the `pipelines-2` project: -

    $ ./gradlew runPipelineTester -Pptargs=-opipelines,pipelines-2.test_probe

### In Docker
You can run the pipeline tests in Docker using their expected container
image (defined in the service descriptor). Doing this gives you added
confidence that your pipeline will work wen deployed.

To execute the tests within their containers run the docker-specific
Gradle task: -

    $ ./gradlew runDockerPipelineTester
    
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
    re-executed and when all the tests pass
-   The files generated by the tests are not deleted until the test
    framework has finished. If your tests generate large output files you
    may need to make sure your disk can accommodate all the output from all
    the tests
-   When you re-run the pipeline tester it removes any remaining collected
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

## Considerations for Windows
>   Windows is not currently supported btu the following may help you
    get started.

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

## Testing the pipeline utilities Python utilities
The pipeline utilities consist of a number of Python-based modules
that provide some common processing but are also distributed in the
pipeline container images. The distributed can be tested using `setup.py`.
To test these modules run the following from the `src/python` directory: -

    $ pip install -r requirements.txt
    $ python setup.py test

# Publishing to PyPI
The utilities are automatically published to [PyPI] for easy installation.

We currently employ Travis to automate our builds and also to publish the
Python module to PyPI. The Travis CI/CD service (controlled by the `.travis.yml`
file) automatically publishes to PyPI when the repository is tagged (normally
on **master**).

In order to publish a new release of the module you need to: -

1.  **Review** the `src/python/READEME.rst` file (which is the source of
    the PyPi documentation). This rarely needs changing but it's worth
    understanding where the PyPI documentation comes from.
3.  **Tag** the repository's **master** branch. It's the tagging that instructs
    Travis to publish the Python module to PyPI - it does not publish *every*
    commit.

>   Before tagging you must ensure that the tag you're using has not
    been used before. If you re-publish a package to PyPI it will respond with
    an error if the version's been published before. Once you
    have released version 1.0.0 you cannot release it again. Ever.
    
>   For acceptable version number formatting you should follow [PEP-440].

---

[Conda]: https://conda.io/docs/
[Git for Windows]: http://gitforwindows.org
[Gradle]: https://gradle.org
[Groovy]: http://groovy-lang.org
[JAVA_HOME]: https://docs.oracle.com/cd/E19182-01/820-7851/inst_cli_jdk_javahome_t/
[PEP-440]: https://www.python.org/dev/peps/pep-0440/
[PIP]: https://pypi.python.org/pypi
[Pipelines]: https://github.com/InformaticsMatters/pipelines.git
[PyPI]: https://pypi.python.org/pypi
