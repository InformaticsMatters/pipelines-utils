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

/**
 * The PipelineTester Executor class. The class is responsible for executing
 * a pipeline command using the command-line.
 */
class ShellExecutor {

    /**
     * Executes the given command in the supplied execution directory.
     *
     * @param command The command to run
     * @param edir The directory in which to run the command
     * @param pin The pipeline input directory
     *            (used to define the PIN environment variable)
     * @param pout The pipeline output directory
     *             (used to define the POUT environment variable)
     * @param timeoutSeconds The time to allow for the command to execute
     * @return A list containing the STDOUT and STDERR encapsulated in a
     *         StringBuilder(), an integer command exit code and a timeout
     *         boolean (set if the program execution timed out)
     */
    static execute(String command, File edir,
                   String pin, String pout,
                   int timeoutSeconds) {

        StringBuilder sout = new StringBuilder()
        StringBuilder serr = new StringBuilder()
        // Append '/' to PIN and POUT to allow '${POUT}output'
        String cmd = "PIN=$pin/; POUT=$pout/; " + command
        def proc = ['sh', '-c', cmd].execute(null, edir)
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill((long)timeoutSeconds * 1000)
        int exitValue = proc.exitValue()

        // Timeout?
        //
        // Some exit codes have a special meaning.
        //
        // We can, for example, assume that the process was killed
        // if the exit code is 143 as 143, which is 128 + 15, means
        // the program died with signal 15 (SIGTERM).
        // See http://www.tldp.org/LDP/abs/html/exitcodes.html.
        boolean timeout = exitValue == 143 ? true : false

        return [sout, serr, exitValue, timeout]

    }

}
