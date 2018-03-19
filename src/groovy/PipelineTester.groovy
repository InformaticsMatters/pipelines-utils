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

import org.apache.commons.cli.Option

/**
 * The PipelineTester. A groovy script for the automated testing
 * of Informatics Matters pipeline scripts. This utility searches for
 * pipeline `.test` files and then loads and executes them.
 *
 *      `pipeline.test.template` is template that can be used as to create your
 *      own tests. Read its inline documentation to understand how it is used
 *      within this framework.
 *
 * Depending on your pipeline's requirements you may need to execute this
 * the PipelineTester within a suitable execution environment like Conda.
 */

// Version
// Update with every change/release
String version = '2.4.3'

println "+------------------+"
println "|  PipelineTester  | v$version"
println "+------------------+"

// Build command-line processor
// and then parse the command-line...
def cli = new CliBuilder(usage:'groovy PipelineTester.groovy',
                         stopAtNonOption:false)
cli.with {
    v longOpt: 'verbose', "Display the pipeline's log"
    d longOpt: 'indocker', "Run tests using their container images"
    s longOpt: 'stoponerror', "Stop executing on the first test failure"
    h longOpt: 'help', "Print this message"
    o longOpt: 'only', args: Option.UNLIMITED_VALUES, argName: 'directory',
                valueSeparator: ',', "Comma-separated list of test directories"
}
def options = cli.parse(args)
if (!options) {
    System.exit(1)
}
if (options.help) {
    cli.usage()
    return
}

// Provide 'only' default (can't seem to do it with CliBuilder)
def only = []
if (options.o) {
    only.addAll(options.os)
}
// Create a Tester object
// and run all the tests that have been discovered...
Tester pipelineTester = new Tester(verbose:options.v,
                                   inDocker:options.d,
                                   stopOnError:options.s,
                                   onlySpec:only)
boolean testResult = pipelineTester.run()

// Leave with a non-zero exit code on failure...
if (!testResult) {
    System.exit(1)
}
