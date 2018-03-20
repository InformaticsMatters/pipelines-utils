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

import java.nio.file.Files
import java.util.regex.Pattern

import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine

/**
 * The Tester. A groovy class for the automated testing
 * of Informatics Matters pipeline scripts. This class searches for
 * pipeline test files and then loads and executes them.
 */
class Tester {

    // Supported test-script versions
    def supportedTestFileVersions = [1]

    // Execution OS
    String osName = System.properties['os.name']

    // Controlled by command-line
    boolean verbose = false
    boolean inDocker = true
    boolean stopOnError = false
    def onlySpec = []

    // Constants?
    int defaultTimeoutSeconds = 60
    String testExt = '.test'
    String executeAnchorDir = File.separator + 'src' + File.separator
    String testFileSpec = '**' + File.separator + "*${testExt}"
    String testSearchDir = '..'+ File.separator + '..' + File.separator + '..'
    String sdExt = '.dsd.json'
    String optionPrefix = 'arg.'
    String metricsFile = 'output_metrics.txt'
    String outputRegex = '-o (\\S+)'
    String dumpOutPrefix = '   #'
    String dumpErrPrefix = '   |'
    String setupPrefix = 'setup_'
    String testPrefix = 'test_'
    String ignorePrefix = 'ignore_'

    final static String defaultInputPath = '..' + File.separator + '..' + File.separator + 'data'
    final static String defaultOutputPath = System.getProperty('user.dir') +
            File.separator + 'tmp' + File.separator + 'PipelineTester-out'
    final static String alternativeInputPath = System.getProperty('user.dir') +
            File.separator + 'tmp' + File.separator + 'PipelineTester-in'

    // Controlled by setup sections
    int testTimeoutSeconds = defaultTimeoutSeconds

    // Material accumulated as the tests execute
    int testScriptVersion = 0
    int sectionNumber = 0
    int numTestsFound = 0
    int numTestsExcluded = 0
    int testsIgnored = 0
    int testsSkipped = 0
    int testsPassed = 0
    int filesUsed = 0
    def failedTests = []
    def observedFiles = []
    // The set of directories (pipeline repos) that contained test files.
    Set observedDirectories = new HashSet()

    // The service descriptor for the current test file...
    def currentServiceDescriptor = null
    // The current pipeline test directory
    String currentTestDirectory = ''
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
    // The input directory is based on location of the test file.
    // The output directory is based on where the PipelineTester is executed from.
    // The base output directory can be redirected with the POUT environment variable
    // which is copied to env_pout.
    String test_pin = null
    String test_pout = null
    // The POUT environment variable (or null)
    // Set in the static initialiser.
    final static String env_pout

    // A filename checker for files generated for the service descriptor.
    // Used for service-descriptor pipelines to make sure the
    // files they are expected to generate are actually generated.
    MediaChecker mediaChecker = new MediaChecker()

    /**
     * The run method.
     * Locates all the test files, loads them and executes them.
     * This is the main/public entry-point for the class.
     *
     * @return boolean, false if any test has failed.
     */
    boolean run() {

        // Log supported test file versions
        Log.info('Tests', "Supporting test file versions: $supportedTestFileVersions")
        Log.info('Stop on error', stopOnError)
        Log.info('OS Name', osName)

        // Only process some directories?
        onlySpec.each {
            Log.info('Only', it)
        }

        // List of directories skipped,
        // used if the user has specified 'only'
        def skippedDirectories = []

        // Before we start - cleanup (everything)
        cleanUpOutput()

        // Find all the potential test files
        String searchRoot = new File(testSearchDir).getCanonicalPath()
        def testFiles = new FileNameFinder().getFileNames(searchRoot, testFileSpec)
        for (String path : testFiles) {

            // Keep every new root directory where a test file was found...
            // But optionally skip if this directory if the user's specified
            // an 'only' list and this directory is not in it.
            String testDir = path[searchRoot.size()..-1].split(Pattern.quote(File.separator))[1]
            if (avoid(testDir)) {
                if (!skippedDirectories.contains(testDir)) {
                    Log.separate()
                    Log.info('Not in only', testDir)
                    skippedDirectories.add(testDir)
                }
                continue
            }

            // Add to our list of observed directories
            observedDirectories.add(testDir)
            currentTestDirectory = testDir

            // Reset filename and section number
            // along with other test-specific objects
            currentTestFilename = path.split(Pattern.quote(File.separator))[-1]
            currentTestFilename = currentTestFilename.
                    take(currentTestFilename.length() - testExt.length())
            sectionNumber = 0
            testScriptVersion = 0
            testTimeoutSeconds = defaultTimeoutSeconds
            collectionCreates = []
            filesUsed += 1

            Log.separate()
            Log.info('Test file', currentTestFilename)
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
                        Log.separate()
                        Log.err("Unsupported test script version ($section.value)." +
                                " Expected value from choice of $supportedTestFileVersions")
                        Log.err("In $path")
                        recordFailedTest("-")
                        break
                    }

                } else {

                    // Must have a version number if we get here...
                    if (testScriptVersion == 0) {
                        Log.separate()
                        Log.err('The file is missing its version definition')
                        recordFailedTest("-")
                        break
                    }

                    // Section is either a `setup_collect` or `test`...
                    sectionNumber += 1
                    if (section_key_lower.startsWith(setupPrefix)) {
                        processSetupCollection(section)
                    } else if (section_key_lower.startsWith(testPrefix)) {
                        // Should we avoid this test?
                        if (!avoid(testDir, section_key_lower)) {
                            processTest(path, section)
                        }
                    } else if (section_key_lower.startsWith(ignorePrefix)) {
                        Log.separate()
                        logTest(path, section)
                        testsIgnored += 1
                        Log.info('Result', 'Ignored')
                    } else {
                        Log.separate()
                        Log.err("Unexpected section name ($section.key)" +
                                " in the '${currentTestFilename}.test'")
                        recordFailedTest(section_key_lower)
                    }

                    if (stopOnError && failedTests.size() > 0) {
                        break
                    }

                }

                if (stopOnError && failedTests.size() > 0) {
                    break
                }

            }

            if (stopOnError && failedTests.size() > 0) {
                break
            }

        }

        // Cleanup if successful.
        // We'll also cleanup again when we re-run.
        boolean testPassed = false
        if (failedTests.size() == 0) {
            Log.separate()
            cleanUpOutput()
            testPassed = true
        }

        // Summarise...

        Log.separate()
        Log.info('Summary', '')

        // List observed directories
        if (skippedDirectories) {
            Log.separate()
            skippedDirectories.each { name ->
                Log.info('Skipped directory', name)
            }
        }
        // List observed directories
        if (observedDirectories) {
            Log.separate()
            observedDirectories.each { name ->
                Log.info('Test directory', name)
            }
        }
        // List failed tests...
        if (failedTests) {
            Log.separate()
            failedTests.each { name ->
                Log.info('Failed', name)
            }
        }

        Log.separate()
        int testsFailed = failedTests.size()
        Log.info('Test files', sprintf('%3s', filesUsed ? filesUsed : '-'))
        Log.info('Tests found', sprintf('%3s', numTestsFound ? numTestsFound : '-'))
        Log.info('Tests passed',sprintf('%3s', testsPassed ? testsPassed : '-'))
        Log.info('Tests failed', sprintf('%3s', testsFailed ? testsFailed : '-'))
        Log.info('Tests skipped', sprintf('%3s', testsSkipped ? testsSkipped : '-'))
        Log.info('Tests ignored', sprintf('%3s', testsIgnored ? testsIgnored : '-'))
        Log.info('Tests excluded', sprintf('%3s', numTestsExcluded ? numTestsExcluded : '-'))
        Log.separate()
        if (testsFailed) {
            Log.info('Result', 'FAILURE')
        } else {
            Log.info('Result', 'SUCCESS')
        }
        Log.separate()

        return testPassed

    }

    /**
     * Returns true if the tester should avoid tests in this directory.
     *
     * @param directory The directory we're about to process
     * @return True to avoid running in this directory
     */
    private boolean avoid(String directory) {

        // If there's nothing in the 'only' list then we don't
        // need to avoid anything
        if (!onlySpec) {
            return false
        }

        for (String spec : onlySpec) {
            if (spec.tokenize('.')[0].equals(directory)) {
                return false
            }
        }
        // The named directory is not in the list.
        // We should avoid this directory.
        return true

    }

    /**
     * Returns true if the tester should avoid this test
     * (in the supplied directory).
     *
     * @param directory The directory we're processing
     * @param test The test we're about to process
     * @return True to avoid running in this directory
     */
    private boolean avoid(String directory, String test) {

        // If there's nothing in the 'only' list then we don't
        // need to avoid anything
        if (!onlySpec) {
            return false
        }

        // The spec is the combination of directory and test.
        // i.e. dir='one' and test='two' has a spec of 'one.two'
        String spec = directory + '.' + test
        // This test can execute if:
        // the directory is in the only list or the spec is.
        if (onlySpec.contains(directory) || onlySpec.contains(spec)) {
            return false
        }
        // The test is not in the only list.
        // We must avoid this test.
        return true

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

        Log.info('Cleanup', 'Cleaning collected output')

        // If the output path exists, remove it.
        File tmpPath = new File(env_pout ? env_pout : defaultOutputPath)
        if (tmpPath.exists()) {
            if (!tmpPath.isDirectory()) {
                Log.err("Output directory exists but it's not a directory " +
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
     * Dumps the pipeline command's output, used when there's been an error.
     */
    private dumpCommandError(StringBuilder errString) {

        println "$dumpErrPrefix STDERR Follows..."
        errString.toString().split('\n').each { line ->
            println "$dumpErrPrefix $line"
        }

    }

    /**
     * Dumps the pipeline command's output, used when the user's used '-v'.
     */
    private dumpCommandOutput(StringBuilder outString) {

        println "$dumpOutPrefix STDOUT Follows..."
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

            // 'arg.threshold` is recorded as `threshold`
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
                    Log.err("Pipeline option '$option' is not defined in the test's params" +
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
            Log.err("Test param '$paramName' is not a recognised pipeline option")
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

        // Just return the original text if there are no values....
        if (values == null) {
            return text
        }

        // Cope with missing parameter expansion.
        // This allows us to replace legitimate user values
        // like --dose \"$doses\" but preserve system values
        // like ${PIN}. If we don't do this we'll get a
        // groovy.lang.MissingPropertyException with something like
        // "No such property: PIN"
        def binding = values.withDefault { x -> '${' + x + '}' }

        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(text).make(binding)
        return template.toString()

    }

    /**
     * Processes the setup_collection section.
     */
    def processSetupCollection(setupSection) {

        Log.separate()
        Log.info('Action', 'Processing setup_collection section')

        // Extract key setup values, supplying defaults
        if (setupSection.value.timeout != null) {
            int timeoutSeconds = setupSection.value.get('timeout')
            if (timeoutSeconds != null) {
                Log.info('Setup', "timeout=$timeoutSeconds")
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

        failedTests.add("${currentTestDirectory}/${currentTestFilename}.${testName}")

    }

    /**
     * Logs key information about the current test.
     *
     * @param path Full path to the test file
     * @param section The test section
     */
    private logTest(String path, def section) {

        Log.info('Test', section.key.toString())
        Log.info('File', currentTestFilename)
        Log.info('Path', path)

    }

    /**
     * Creates a temporary directory and links the defined input files
     * into that directory using the 'expected' filename. This method is
     * called if the test being executed has a map of input test
     * file replacements (i.e. an 'inputBlock').
     *
     * @param pin The test's default data inout directory
     * @param command The command that will be executed
     * @param inputBlock The user's input block definition.
     * @return A String representing the new data directory (null on failure).
     */
    private createTestInputData(String pin, String command, def inputBlock) {

        boolean success = true

        // Create the 'alternative' input directory (after deleting it).
        // Test files wil be copied/linked to here.
        destroyTestInputDataDir()
        new File(alternativeInputPath).mkdir()

        // Link each test file (inputBlock.item.value) to the expected file
        // (inputBlock.item.key). The input will be in the
        // the current 'PIN' and needs to be linked to the new (temporary)
        // directory.
        inputBlock.each { item ->
            // The expected file must be in the command.
            // i.e. if the user expects to replace input.data.gz with
            // their own file the command must contain the string
            // '{PIN}input.data.gz'
            String iString = "\\{PIN\\}" + item.key.toString()
            def iOption = command =~ /$iString/
            if (iOption.count == 0) {
                // Report every file that's missing...
                Log.err("Alternative inputs defined" +
                        " but the command has no '\${PIN}${item.key}' input")
                if (success) {
                    destroyTestInputDataDir()
                    success = false
                }
            }
            // Does the source file exist?
            File src = new File(pin + File.separator + item.value)
            if (!src.exists()) {
                // Report every file that's missing...
                Log.err("Alternative inputs defined" +
                        " but the alternative file does not exist" +
                        " (${src.toString()})")
                if (success) {
                    destroyTestInputDataDir()
                    success = false
                }
            }
            // If all is still OK then link...
            if (success) {
                File dst = new File(alternativeInputPath.toString() + File.separator + item.key)
                Files.createSymbolicLink(dst.toPath(), src.toPath())
            }
        }

        return success ? alternativeInputPath : null

    }

    /**
     * Removes temporary data.
     */
    private destroyTestInputDataDir() {

        new File(alternativeInputPath).deleteDir()

    }

    /**
     * Replaces key items in a string that may be confused with
     * regular expression syntax to simplify (?) string comparisons
     * for the tester.
     *
     * @param stringValue The string that needs 'sanitising'
     * @return New reg-ex-friendly string
     */
    private static escapeString(stringValue) {

        // Escape brackets
        String escapedString = stringValue.replaceAll(/\(/, '\\\\(')
        escapedString = escapedString.replaceAll(/\)/, '\\\\)')

        // Replace spaces in the strings
        // with a simple _variable whitespace_ regex (excluding
        // line-breaks and form-feeds).
        // This simplifies the user's world so they can avoid the
        // pitfalls of regular expressions and can use
        // 'Cmax 0.42' rather than thew rather un-human
        // 'Cmax\\s+0.42' for example.
        // Of course they can always use regular expressions.
        escapedString = escapedString.replaceAll(/\s+/, '[ \\\\t]+')

        return escapedString

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

        numTestsFound += 1

        Log.separate()
        logTest(filename, section)

        def command = section.value['command']
        def paramsBlock = section.value['params']
        def stderrBlock = section.value['stderr']
        def stdoutBlock = section.value['stdout']
        def inputBlock = section.value['input']
        def createsBlock = section.value['creates']
        def metricsBlock = section.value['metrics']
        def excludeBlock = section.value['exclude']

        // Is this OS excluded?
        for (String exclusion in excludeBlock) {
            if (osName ==~ exclusion) {
                Log.info('Excluding', exclusion)
                // Increment the number of excluded tests
                numTestsExcluded += 1
                return
            }
        }

        // Unless a test-specific command has been defined
        // check the parameters against the service descriptor
        // to ensure that all the options are recognised.
        String pipelineCommand
        if (command == null) {

            if (currentServiceDescriptor == null) {
                Log.err('Found "params" but there was no service descriptor file.')
                recordFailedTest(section.key)
                return
            } else if (!checkAllOptionsHaveBeenUsed(paramsBlock)) {
                recordFailedTest(section.key)
                return
            }

            // No raw command defined in the test block,
            // so use the command defined in the service descriptor...
            pipelineCommand = currentServiceDescriptor.command

        } else {

            // The user-supplied command might be a multi-line string.
            // Flatten it...
            String the_command = ''
            command.eachLine {
                the_command += it.trim() + ' '
            }
            pipelineCommand = the_command.trim()

        }

        // Replace the respective values in the command string...
        pipelineCommand = expandTemplate(pipelineCommand, paramsBlock)
        // Replace newlines with '\n'
        pipelineCommand = pipelineCommand.replace(System.lineSeparator(), '\n')

        // Try to construct an image name (appending ':latest' when required)
        imageName = (currentServiceDescriptor != null) ? currentServiceDescriptor.imageName : null
        if (imageName) {
            imageName = (imageName =~ /:/) ? imageName : imageName + ':latest'
        }
        Log.info('Image', imageName)

        // If we're running 'inDocker' and there's no 'imageName'
        // there's no point in continuing.
        // We simply record this as a skipped test.
        if (inDocker && imageName == null) {
            Log.info('Skip', 'Yes')
            testsSkipped += 1
            return
        }

        // Here ... `pipelineCommand` is either the SD-defined command or the
        // command defined in this test's command block.

        // The pipeline execution directory is the directory _below_
        // the path's `executeAnchorDir` directory. The anchor directory
        // is, by default, '/src/'.
        // If the test's full path is: -
        //      /Users/user/pipelines-a/src/python/pipelines/tmp/tmp.test
        // The execution directory is: -
        //      /Users/user/pipelines-a/src/python
        //
        // This is also used to set the PROOT environment variable
        // expected by the test when run in the ShellExecutor.
        //
        int executeAnchorDirPos = filename.indexOf(executeAnchorDir)
        String executeDir = filename.take(filename.indexOf(File.separator,
                executeAnchorDirPos + executeAnchorDir.length()))
        File testExecutionDir = new File(executeDir)

        String testSubDir = "${currentTestFilename}-${section.key}"

        // POUT is either set by the POUT environment variable or relative
        //      to the directory we've been executed from.
        test_pout = env_pout ? env_pout : defaultOutputPath
        test_pout += File.separator + testSubDir

        // Construct and make the path for any '-o' output
        File testOutputPath = new File(test_pout)
        testOutputPath.mkdir()

        // Redirect the '-o' option, if there is a '-o' in the command
        def oOption = pipelineCommand =~ /$outputRegex/
        if (oOption.count > 0) {

            // Redirect output
            String outputFileBaseName = oOption[0][1]
            String testOutputFile = '\\${POUT}' + outputFileBaseName
            // Now swap-out the original '-o'...
            String redirectedOutputOption = "-o ${testOutputFile}"
            pipelineCommand = pipelineCommand.replaceAll(/$outputRegex/,
                    redirectedOutputOption)

        }

        Log.info('Command', pipelineCommand)

        // PIN (Pipeline input data) is normally the project's data directory.
        // If the user has defined an 'input' block in their test file
        // the test utility will create a temporary directory so that the
        // user's alternative input files can be linked safely into it.
        test_pin = new File(executeDir, defaultInputPath).getCanonicalPath()
        if (inputBlock) {
            // This call does some validation. If all looks well then
            // we'll get back a path. If there are problems we'll get null.
            test_pin = createTestInputData(test_pin, pipelineCommand, inputBlock)
            if (test_pin == null) {
                recordFailedTest(section.key)
                return
            }
        }

        Log.info('Run path (PROOT)', testExecutionDir.toString())
        Log.info('Input path (PIN)', test_pin)
        Log.info('Output path (POUT)', test_pout)

        if (numTestsFound > 1) {
            // Report on the current test status - has anything failed?
            String okSoFar = failedTests ? 'No' : 'Yes'
            Log.info('Testing OK so far', okSoFar)
        }

        // Execute the command, using the shell, giving it time to complete,
        // while also collecting stdout & stderr
        StringBuilder sout
        StringBuilder serr
        int exitValue
        boolean timeout
        if (imageName && inDocker) {
            Log.info('Docker', 'Yes')
            (sout, serr, exitValue, timeout) =
                    ContainerExecutor.execute(pipelineCommand,
                            imageName, test_pin, test_pout, testTimeoutSeconds)

        } else {
            Log.info('Docker', 'No')
            (sout, serr, exitValue, timeout) =
                    ShellExecutor.execute(pipelineCommand,
                            testExecutionDir, test_pin, test_pout, testTimeoutSeconds)

        }

        // Dump pipeline output (written to stderr) if verbose
        if (verbose) {
            dumpCommandOutput(serr)
        }

        // Remove any temporary input directory
        // (created if the user defined an `input` block)
        if (inputBlock) {
            destroyTestInputDataDir()
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

            // Check service descriptor output files (unless its a raw command).
            // If the service descriptor has one or more `outputDescriptors`
            // Then we should check that the pipeline generated the
            // expected files using the MediaChecker.
            if (currentServiceDescriptor != null && command == null) {
                validated = mediaChecker.check(currentServiceDescriptor, testOutputPath)
            }

            // Do we expect additional output files?
            // Here we look for things like "output*" in the
            // redirected output path.
            if (validated && testCreates.size() > 0) {
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
                        Log.err("Expected output file '$expectedFile' but couldn't find it")
                        validated = false
                        break
                    }
                }
            }

            // There are optional stderr and stdout blocks.
            // Has the user asked us to check any stderr text
            // that has been logged?
            if (validated && stderrBlock != null) {

                // Check that we see everything the test tells us to see.
                stderrBlock.each { see ->
                    String stderrExpr = escapeString(see)
                    def finder = (serr =~ /$stderrExpr/)
                    if (finder.count == 0) {
                        Log.err("Expected to see '$see' but it was not in the command's stderr")
                        validated = false
                    }
                }

            }
            if (validated && stdoutBlock != null) {

                // Check that we see everything the test tells us to see
                // on the stdout stream.
                stdoutBlock.each { see ->
                    String stdoutExpr = escapeString(see)
                    def finder = (sout =~ /$stdoutExpr/)
                    if (finder.count == 0) {
                        Log.err("Expected to see '$see' but it was not in the command's stdout")
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
                            Log.err("Metric for '$metric.key' is not in the metrics file")
                            validated = false
                        } else {
                            String valueExpr = escapeString(metric.value)
                            def finder = (fileProperty =~ /$valueExpr/)
                            if (finder.count == 0) {
                                Log.err("Expected value for metric '$metric.key' ($metric.value)" +
                                        " does not match file value ($fileProperty)")
                                validated = false
                            }
                        }
                    }
                } else {
                    Log.err("Expected metrics but there was no metrics file ($metricsFile)")
                    validated = false
                }
            }

        } else {
            if (timeout) {
                Log.err("Execution was terminated" +
                    " (taking longer than ${testTimeoutSeconds}S)")
            }
            Log.err("Pipeline exitValue=$exitValue. <stderr> follows...")
        }

        if (exitValue == 0 && validated) {
            testsPassed += 1
            Log.info('Result', 'SUCCESS')
        } else {
            // Test failed.
            dumpCommandOutput(sout)
            dumpCommandError(serr)
            recordFailedTest(section.key)
        }

    }

    /**
     * Static initialiser.
     */
    static {

        def env = System.getenv()
        env_pout = env['POUT']

    }

}
