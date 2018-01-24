#!/usr/bin/env groovy

/**
 * Copyright (c) 2018 Informatics Matters Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine

import ShellExecutor

/**
 * The Tester. A groovy class for the automated testing
 * of Informatics Matters pipeline scripts. This class searches for
 * pipeline test files and then loads and executes them.
 */
class Tester {

    // Supported test-script versions
    def supportedTestFileVersions = [1]

    // Controlled by command-line
    boolean verbose = false
    boolean inDocker = true

    // Constants?
    int defaultTimeoutSeconds = 1
    String testExt = '.test'
    String executeAnchorDir = '/src/'
    String testFileSpec = "**/*${testExt}"
    String testSearchDir = '../../..'
    String sdExt = '.dsd.json'
    String optionPrefix = 'arg.'
    String metricsFile = 'output_metrics.txt'
    String outputRegex = '-o (\\S+)'
    String dumpOutPrefix = '   #'
    String dumpErrPrefix = '   |'
    String setupPrefix = 'setup_'
    String testPrefix = 'test_'
    String ignorePrefix = 'ignore_'

    final static String defaultInputPath = '../../data'
    final static String defaultOutputPath = System.getProperty('user.dir') + '/tmp/PipelineTester'

    // Controlled by setup sections
    int testTimeoutSeconds = defaultTimeoutSeconds

    // Material accumulated as the tests execute
    int testScriptVersion = 0
    int sectionNumber = 0
    int testsFound = 0
    int testsIgnored = 0
    int testsSkipped = 0
    int testsPassed = 0
    int filesUsed = 0
    int numWarnings = 0
    def failedTests = []
    def observedFiles = []

    // The service descriptor for the current test file...
    def currentServiceDescriptor = null
    // The current test filename (short)
    String currentTestFilename = ''
    // A convenient list of normalised service descriptor option names
    // i.e. 'arg.volumes' becomes 'volumes' (also contains expanded ranges)
    def optionNames = []
    def optionDefaults = [:]
    // Files created by the entire test collection.
    // Defined in the 'setup_collection.creates' block.
    def collectionCreates = []
    // Docker image name
    // (for pipelines that normally execute in a container)
    String imageName = null
    // The Pipeline IN and OUT directories.
    // Taken from environment variables (poin/pout) or test-specific values
    // from the setup_collection block.
    String test_pin = null
    String test_pout = null

    /**
     * The run method.
     * Locates all the test files, loads them and executes them.
     * This is the main/public entry-point for the class.
     *
     * @return boolean, false if any test has failed.
     */
    boolean run() {

        // Log supported test file versions
        info("Supporting test file versions: $supportedTestFileVersions")

        // Before we start - cleanup (everything)
        cleanUpOutput()

        // Find all the potential test files
        def testFiles = new FileNameFinder().getFileNames(testSearchDir, testFileSpec)
        for (String path : testFiles) {

            // Reset filename and section number
            // along with other test-specific objects
            currentTestFilename = path.split(File.separator)[-1]
            currentTestFilename = currentTestFilename.
                    take(currentTestFilename.length() - testExt.length())
            sectionNumber = 0
            testScriptVersion = 0
            testTimeoutSeconds = 30
            collectionCreates = []
            filesUsed += 1

            separate()
            info("File: $currentTestFilename")
            // Collect the files we find...
            if (!observedFiles.contains(currentTestFilename)) {
                observedFiles.add(currentTestFilename)
            }

            // Guess the Service Descriptor path and filename
            // and try to extract the command and the supported options.
            // The SD file must exist if the user has defined a set of
            // parameters. The SD file is not required if the test
            // contains just raw commands.
            String sdFilename = path.take(path.length() - testExt.length()) + sdExt
            currentServiceDescriptor = null
            File sdFilenameFile = new File(sdFilename)
            if (sdFilenameFile.exists()) {
                currentServiceDescriptor = new JsonSlurper().parse(sdFilenameFile.toURI().toURL())
                extractOptionsFromCurrentServiceDescriptor()
            }

            // Now run each test found in the test spec
            // (also checking for `setup_collection` and `version` sections).
            def test_spec = new ConfigSlurper().parse(new File(path).toURI().toURL())
            for (def section : test_spec) {

                String section_key_lower = section.key.toLowerCase()
                if (section_key_lower.equals('version')) {

                    if (!checkFileVersion(section.value)) {
                        separate()
                        err("Unsupported test script version ($section.value)." +
                            " Expected value from choice of $supportedTestFileVersions")
                        err("In $path")
                        recordFailedTest("-")
                        break
                    }

                } else {

                    // Must have a version number if we get here...
                    if (testScriptVersion == 0) {
                        separate()
                        err('The file is missing its version definition')
                        recordFailedTest("-")
                        break
                    }

                    // Section is either a `setup_collect` or `test`...
                    sectionNumber += 1
                    if (section_key_lower.startsWith(setupPrefix)) {
                        processSetupCollection(section)
                    } else if (section_key_lower.startsWith(testPrefix)) {
                        processTest(path, section)
                    } else if (section_key_lower.startsWith(ignorePrefix)) {
                        separate()
                        logTest(path, section)
                        testsIgnored += 1
                        info('OK (Ignored)')
                    } else {
                        separate()
                        err("Unexpected section name ($section.key)" +
                            " in the '${currentTestFilename}.test'")
                        recordFailedTest(section_key_lower)
                    }

                }

            }

        }

        separate()
        info('Done')

        // Cleanup if successful.
        // We'll also cleanup again when we re-run.
        boolean testPassed = false
        if (failedTests.size() == 0) {
            cleanUpOutput()
            testPassed = true
        }

        // Summarise...

        separate()
        println "Summary"
        separate()
        if (failedTests.size() > 0) {
            failedTests.each { name ->
                println "Failed: $name"
            }
        }
        println "Test Files    : " + sprintf('%4d', filesUsed)
        println "Tests Found   : " + sprintf('%4d', testsFound)
        println "Tests passed  : " + sprintf('%4d', testsPassed)
        println "Tests failed  : " + sprintf('%4d', failedTests.size())
        println "Tests skipped : " + sprintf('%4d', testsSkipped)
        println "Tests ignored : " + sprintf('%4d', testsIgnored)
        println "Warnings      : " + sprintf('%4d', numWarnings)
        separate()
        println "Passed: ${testPassed.toString().toUpperCase()}"

        return testPassed

    }

    /**
     * Checks the file version supplied is supported.
     * If successful the `testScriptVersion` member is set.
     *
     * @param version The version (*number)
     * @return false on failure
     */
    private boolean checkFileVersion(version) {

        if (supportedTestFileVersions.contains(version)) {
            testScriptVersion = version
            return true
        }
        return false

    }

    /**
     * Cleanup the generated output.
     *
     * Pipelines that create files have their files placed in the project's
     * `tmp` directory. This method, called at the start of testing and when
     * all tests have passed successfully, removes the collected files.
     */
    private cleanUpOutput() {

        info("Cleaning collected output")

        // If the output path exists, remove it.
        File tmpPath = new File(defaultOutputPath)
        if (tmpPath.exists()) {
            if (!tmpPath.isDirectory()) {
                err("Output directory exists but it's not a directory " +
                        "(${tmpPath.toString()})")
                return false
            } else {
                // Delete sub-dirs and files.
                tmpPath.deleteDir()
            }
        }
        // Finally, (re-)create the output directory path...
        tmpPath.mkdirs()

    }

    /**
     * Print a simple separator (a bar) to stdout.
     * Used to visually separate generated output into logical blocks.
     */
    private separate() {

        println "-------"

    }

    /**
     * Print a message prefixed with th e`infoPrompt` string.
     */
    static private info(String msg) {

        println "-> $msg"

    }

    /**
     * Print an error message.
     */
    static private err(String msg) {

        println "ERROR: $msg"

    }

    /**
     * Print a warning message (and counts it).
     */
    private warning(String msg) {

        println "WARNING: $msg"
        numWarnings += 1

    }

    /**
     * Dumps the pipeline command's output, used when there's been an error.
     */
    private dumpCommandError(StringBuilder errString) {

        errString.toString().split('\n').each { line ->
            System.err.println "$dumpErrPrefix $line"
        }

    }

    /**
     * Dumps the pipeline command's output, used when the user's used '-v'.
     */
    private dumpCommandOutput(StringBuilder outString) {

        outString.toString().split('\n').each { line ->
            println "$dumpOutPrefix $line"
        }

    }

    /**
     * Extracts the service descriptor option names and default values.
     * It automatically adds `minValue` and `maxValue` for ranges.
     */
    private extractOptionsFromCurrentServiceDescriptor() {

        // Clear the current list of option names
        // (to avoid contamination from a prior test)
        optionNames.clear()
        optionDefaults.clear()

        currentServiceDescriptor.serviceConfig.optionDescriptors.each { option ->

            // 'arg.threshold` is recorded as `thrshold`
            String arglessOption = option.key.substring(optionPrefix.length())
            if (option.typeDescriptor.type =~ /.*Range.*/) {
                optionNames.add(arglessOption + '.minValue')
                optionNames.add(arglessOption + '.maxValue')
            } else {
                optionNames.add(arglessOption)
            }

            // Collect the optional default value?
            if (option.defaultValue != null) {
                if (option.defaultValue in java.util.List) {
                    // Assume something like '[java.lang.Float, 0.7]'
                    // So the default's the 2nd entry?
                    optionDefaults[arglessOption] = option.defaultValue[1]
                } else {
                    // Just a string?
                    optionDefaults[arglessOption] = option.defaultValue
                }
            }

        }

    }

    /**
     * Checks that the user's test `params` uses all the options
     * (extracted from the service descriptor), using default values
     * for missing parameter values where appropriate.
     *
     * @return false if an option has not been defined in the test.
     *         The params object may be modified by this method
     *         (default values will be inserted).
     */
    private boolean checkAllOptionsHaveBeenUsed(def params) {

        // Insert default values into the params object
        // for options that have them where a value is not already present
        // in the params...
        optionDefaults.each { defaultValue ->
            if (!params.containsKey(defaultValue.key)) {
                params[defaultValue.key] = defaultValue.value
            }
        }

        // Check that the user has not specified an option
        // that is not in the service descriptor's set of options.
        boolean checkStatus = true
        params.each { param ->
            String paramName = param.key
            if (param.value instanceof LinkedHashMap) {
                // A min/max?
                // Append each key in the sub-list...
                param.value.each { range ->
                    if (!checkParamIsOption(paramName + '.' + range.key)) {
                        checkStatus = false
                    }
                }
            } else {
                if (!checkParamIsOption(paramName)) {
                    checkStatus = false
                }
            }
        }

        if (checkStatus) {
            // Now check that the user has not missed an option.
            optionNames.each { option ->
                boolean foundParam = false
                params.each { param ->
                    // Accommodate range parameters
                    String paramName = param.key
                    if (param.value instanceof LinkedHashMap) {
                        // A min/max?
                        // Append each key and check whether it's an option...
                        param.value.each { range ->
                            if (option == paramName + '.' + range.key) {
                                foundParam = true
                            }
                        }
                    } else {
                        if (option == paramName) {
                            foundParam = true
                        }
                    }
                }
                if (!foundParam) {
                    err("Pipeline option '$option' is not defined in the test's params" +
                            " and there is no default value to use.")
                    checkStatus = false
                }
            }
        }

        // OK if we get here...
        return checkStatus

    }

    /**
     * Checks that an individual `param` uses a known option
     *
     * @return false if the param is not known as an option
     */
    private boolean checkParamIsOption(def paramName) {

        // Checks that the given parameter name
        // is in the service descriptor's set of options.
        if (!optionNames.contains(paramName)) {
            err("Test param '$paramName' is not a recognised pipeline option")
            return false
        }
        return true

    }

    /**
     * Expands the command string with the given parameter values.
     *
     * @return A string where the param variables have been replaced by the
     *          values in the test's parameter
     */
    private static String expandTemplate(String text, def values) {

        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(text).make(values)
        return template.toString()

    }

    /**
     * Processes the setup_collection section.
     */
    def processSetupCollection(setupSection) {

        separate()
        info("Act : Processing setup_collection section")

        // Extract key setup values, supplying defaults
        if (setupSection.value.timeout != null) {
            int timeoutSeconds = setupSection.value.get('timeout')
            if (timeoutSeconds != null) {
                info("Set : timeout=$timeoutSeconds")
                testTimeoutSeconds = timeoutSeconds
            }
        }

        // Globally-defined created files?
        if (setupSection.value.creates != null) {
            collectionCreates = setupSection.value.get('creates')
        }

    }

    /**
     * Adds the test (and its filename) to the list of failed tests.
     *
     * @param testName The test name (test section value).
     */
    private recordFailedTest(testName) {

        failedTests.add("${currentTestFilename}/${testName}")

    }

    /**
     * Logs key information about the current test.
     *
     * @param path Full path to the test file
     * @param section The test section
     */
    private logTest(path, section) {

        info("Test: $section.key")
        info("File: $currentTestFilename")
        info("Path: $path")

    }

    /**
     * Processes (runs) an individual test.
     *
     * If the test fails its name is added to the list of failed tests
     * in the `failedTests` member list.
     *
     * A test consists of the following _blocks_: -
     *
     * -    command
     *      Optional. If specified no service-descriptor (SD) tests are made.
     *      Instead it is used an an alternative to the command normally
     *      extracted from the SD.
     *      This and the params block are mutually exclusive.
     *
     * -    params
     *      A list of parameters (options) and values.
     *      This and the command block are mutually exclusive.
     *
     * -    see
     *      An optional list of regular expressions executed against
     *      the pipeline log.
     *
     * -    creates
     *      An optional list of file names (regular expressions).
     *      Entries in this list are added to any defined in the creates
     *      block in the setup_collection section.
     *
     * -    metrics
     *      An optional list of metrics keyword and value checks
     *      (regular expressions).
     **/
    def processTest(filename, section) {

        testsFound += 1

        separate()
        logTest(filename, section)

        def command = section.value['command']
        def paramsBlock = section.value['params']
        def seeBlock = section.value['see']
        def createsBlock = section.value['creates']
        def metricsBlock = section.value['metrics']

        // Enforce conditions on block combinations...
        if (command != null && paramsBlock != null) {
            err('Found "command" and "params". Use one or the other.')
            recordFailedTest(section.key)
            return
        }

        // Unless a test-specific command has been defined
        // check the parameters against the service descriptor
        // to ensure that all the options are recognised.
        String pipelineCommand
        if (command == null) {

            if (currentServiceDescriptor == null) {
                err('Found "params" but there was no service descriptor file.')
                recordFailedTest(section.key)
                return
            } else if (!checkAllOptionsHaveBeenUsed(paramsBlock)) {
                recordFailedTest(section.key)
                return
            }

            // No raw command defined in the test block,
            // so use the command defined in the service descriptor...
            String the_command = currentServiceDescriptor.command

            // Replace the respective values in the command string...
            pipelineCommand = expandTemplate(the_command, paramsBlock)
            // Replace newlines with '\n'
            pipelineCommand = pipelineCommand.replace(System.lineSeparator(), '\n')

        } else {

            // The user-supplied command might be a multi-line string.
            // Flatten it...
            String the_command = ''
            command.eachLine {
                the_command += it.trim() + ' '
            }
            pipelineCommand = the_command.trim()

        }

        // Try to construct an image name (appending ':latest' when required)
        imageName = (currentServiceDescriptor != null) ? currentServiceDescriptor.imageName : null
        if (imageName) {
            imageName = (imageName =~ /:/) ? imageName : imageName + ':latest'
        }
        info("Img : $imageName")

        // If we're running 'inDocker' and there's no 'imageName'
        // there's no point in continuing.
        // We simply record this as a skipped test.
        if (inDocker && imageName == null) {
            info('Skip: Yes')
            testsSkipped += 1
            return
        }

        // Here ... `pipelineCommand` is either the SD-defined command or the
        // command defined in this test's command block.

        // Redirect the '-o' option, if there is a '-o' in the command
        def oOption = pipelineCommand =~ /$outputRegex/
        String testSubDir = "${currentTestFilename}-${section.key}"
        // Construct and make the path for any '-o' output
        File testOutputPath = new File(defaultOutputPath, testSubDir)
        testOutputPath.mkdir()

        if (oOption.count > 0) {

            // Redirect output
            String outputFileBaseName = oOption[0][1]
            String testOutputFile = '\\${POUT}' + File.separator + outputFileBaseName
            // Now swap-out the original '-o'...
            String redirectedOutputOption = "-o ${testOutputFile}"
            pipelineCommand = pipelineCommand.replaceAll(/$outputRegex/,
                    redirectedOutputOption)

        }

        // The pipeline execution directory is the directory _below_
        // the path's `executeAnchorDir` directory. The anchor directory
        // is, by default, '/src/'.
        // If the test's full path is: -
        //      /Users/user/pipelines-a/src/python/pipelines/tmp/tmp.test
        // The execution directory is: -
        //      /Users/user/pipelines-a/src/python
        //
        int executeAnchorDirPos = filename.indexOf(executeAnchorDir)
        String executeDir = filename.take(filename.indexOf(File.separator,
                executeAnchorDirPos + executeAnchorDir.length()))
        File testExecutionDir = new File(executeDir)
        info("Dir : $testExecutionDir")

        test_pin = new File(executeDir, defaultInputPath).getCanonicalPath()
        test_pout = defaultOutputPath + File.separator + testSubDir

        info("PIN : $test_pin")
        info("POUT: $test_pout")

        // Execute the command, using the shell, giving it time to complete,
        // while also collecting stdout & stderr
        StringBuilder sout
        StringBuilder serr
        int exitValue
        boolean timeout
        if (imageName && inDocker) {
            info('Dock: Yes')
            // And the expanded and redirected command...
            info("Cmd : $pipelineCommand")

            (sout, serr, exitValue, timeout) =
                    ContainerExecutor.execute(pipelineCommand,
                            imageName, test_pin, test_pout, testTimeoutSeconds)

        } else {
            info('Dock: No')
            // And the expanded and redirected command...
            info("Cmd : $pipelineCommand")

            (sout, serr, exitValue, timeout) =
                    ShellExecutor.execute(pipelineCommand,
                            testExecutionDir, test_pin, test_pout, testTimeoutSeconds)

        }

        // Dump pipeline output (written to stderr) if verbose
        if (verbose) {
            dumpCommandOutput(serr)
        }

        // If command execution was successful (exit value of 0)
        // then:
        //  - check that any declared _creates_ output files were created
        //  - iterate through any optional _see_ values,
        //    checking that the pipeline log contains the defined text.
        boolean validated = true
        if (exitValue == 0) {

            def testCreates = collectionCreates.collect()
            if (createsBlock != null) {
                testCreates.addAll(createsBlock)
            }
            // Do we expect output files?
            // Here we look for things like "output*" in the
            // redirected output path.
            if (testOutputPath != null && testCreates.size() > 0) {
                def createdFiles = new FileNameFinder().
                        getFileNames(testOutputPath.toString(), "*")
                for (String expectedFile in testCreates.unique()) {
                    boolean found = false
                    for (String createdFile in createdFiles) {
                        if (createdFile.endsWith(expectedFile)) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        err("Expected output file '$expectedFile' but couldn't find it")
                        validated = false
                        break
                    }
                }
            }

            // Has the user asked us to check text that has been logged?
            if (validated && seeBlock != null) {

                // Check that we see everything the test tells us to see.
                seeBlock.each { see ->
                    // Replace spaces in the 'see' string
                    // with a simple _variable whitespace_ regex (excluding
                    // line-breaks and form-feeds).
                    // This simplifies the user's world so they can avoid the
                    // pitfalls of regular expressions and can use
                    // 'Cmax 0.42' rather than thew rather un-human
                    // 'Cmax\\s+0.42' for example.
                    // Of course they can always use regular expressions.
                    String seeExpr = see.replaceAll(/\s+/, '[ \\\\t]+')
                    def finder = (serr =~ /$seeExpr/)
                    if (finder.count == 0) {
                        err("Expected to see '$see' but it was not in the command's output")
                        validated = false
                    }
                }

            }

            // Has the user specified any metrics?
            // These are located in the `output_metrics.txt` file.
            if (validated && metricsBlock != null) {
                // A metrics file must exist.
                Properties properties = new Properties()
                String metricsPath = testOutputPath.toString() + File.separator + metricsFile
                File propertiesFile = new File(metricsPath)
                if (propertiesFile.exists()) {
                    propertiesFile.withInputStream {
                        properties.load(it)
                    }
                    metricsBlock.each { metric ->
                        String fileProperty = properties."$metric.key"
                        if (fileProperty == null) {
                            // The Metric is not in the file!
                            err("Metric for '$metric.key' is not in the metrics file")
                            validated = false
                        } else {
                            def finder = (fileProperty =~ /${metric.value}/)
                            if (finder.count == 0) {
                                err("Expected value for metric '$metric.key' ($metric.value)" +
                                        " does not match file value ($fileProperty)")
                                validated = false
                            }
                        }
                    }
                } else {
                    err("Expected metrics but there was no metrics file ($metricsFile)")
                    validated = false
                }
            }

        } else {
            if (timeout) {
                err("Execution was terminated" +
                    " (taking longer than ${testTimeoutSeconds}S)")
            }
            err("Pipeline exitValue=$exitValue. <stderr> follows...")
        }

        if (exitValue == 0 && validated) {
            testsPassed += 1
            info('OK')
        } else {
            // Test failed.
            dumpCommandError(serr)
            recordFailedTest(section.key)
            println '!!!!!!!!!!!!!!!!!!!!!!!!!'
            println '!!  >> TEST  ERROR <<  !!'
            println '!!!!!!!!!!!!!!!!!!!!!!!!!'
        }

    }

}
