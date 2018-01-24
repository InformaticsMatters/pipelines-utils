# Pipelines Utils
A repository of common **Informatics Matters** _Pipeline_ utilities shared
between a number of _Pipelines_ repositories. As well as containing a number
of modules previously resident in the [Pipelines] repository this repository
contains a [Groovy]-based test framework that interprets a relatively simple
text-file format used to both describe and execute built-in tests.

## Execution environment
You will need: -

-   Groovy
-   Conda

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

### From within a pipeline repository
You will find this repository located as a submodule. When checking the
pipeline out (for example [Pipelines]) you will need to initialise the
submodule: -

    $ git submodule update --init --remote
    
Then to run the test framework (which searches for test files in the contained
repository) you should navigate to the submodule and run the test framework's
Gradle task: -

    $ cd pipelines-utils
    $ ./gradlew runPipelineTester

The tester summarises each test it has found wile also executing it.
When tests fail it logs as much as it can and continues. When all the tests
have finished it prints a summary including a lit of failed tests along with
of the number of test files and individual tests that were executed: -

    -------
    Summary
    -------
    Test Files    :   20
    Tests Found   :   30
    Tests passed  :   30
    Tests failed  :    0
    Tests skipped :    0
    Tests ignored :    0
    Warnings      :    0
    -------
    Passed: TRUE

### From here
If you have working copies of all your pipeline repositories checked-out
in the same directory as this repository you can execute all the tests
across all the repositories by running the tester from here. Simply
run the following Gradle command from here: -

    $ ./gradlew runPipelineTester

### In Docker
You can run the pipeline tests in Docxker using their expected container
image (defined in the service descriptor). Doing this gives you added
confidence that your pipeline will work wen deployed.

To run in Docker you need to add the `-d` or `--indocker` command-line
argument. To pass these variables through Gradle into the pipeline tester
run the tests like this: -

    $ ./gradlew runPipelineTester -Pptargs=-d

Or by using the Docker-specific gradle command:

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
can be found in the enclosing project. Tests need to be placed in the
directory that contains the pipeline they're testing.

As well as copying existing test files you can use the `pipeline.test.template`
file, in the project root, as a documented template file
in order to create a set of tests for a new pipeline.

>   At the moment the tester only permits one test file per pipeline so all 
    tests for a given pipeline need to be composed in one file. 

## Testing the pipeline utilities
The pipeline utilities consist of a number of Python-based modules
that can be tested using `setup.py`. To test these modules run the
following from the `src/python` directory: -

    $ python setup.py test
 
---

[Conda]: https://conda.io/docs/
[Gradle]: https://gradle.org
[Groovy]: http://groovy-lang.org
[PIP]: https://pypi.python.org/pypi
[Pipelines]: https://github.com/InformaticsMatters/pipelines.git
[RDKit]: http://www.rdkit.org
[Submodule]: https://git-scm.com/docs/gitsubmodules
