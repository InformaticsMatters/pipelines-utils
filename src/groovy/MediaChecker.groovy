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
 * The MediaTypes class. A class to provide validation of files produced
 * by an executing pipeline.
 */
class MediaChecker {

    final static String mediaTypesFile = 'MediaTypes.txt'

    // A map of media type and expected file extension(s).
    // This is populated by the constructor, which reads the content
    // of 'mediaTypesFile'.
    def mediaTypesLookup = null

    /**
     * Default constructor.
     * Reads media types config file.
     */
    MediaChecker() {

        mediaTypesLookup = new ConfigSlurper().parse(new File(mediaTypesFile).toURI().toURL())

    }

    /**
     * Checks that a pipeline's expected files exist.
     *
     * @param serviceDescriptor The test service descriptor
     * @param path The test path (where the pipeline's files are to be found)
     * @return True on success, False otherwise.
     */
    boolean check(serviceDescriptor, File path) {

        boolean retVal = true

        // Check output descriptors for expected output file names...
        // Do all, regardless of individual failure.
        def opDescriptors = serviceDescriptor.serviceConfig.outputDescriptors
        opDescriptors.each { desc ->

            // Expected extension...
            // (use the media type to lookup it up)
            // And construct full path to the expected file...
            String opExt = mediaTypesLookup.media_type[desc.mediaType]
            String opName = desc.name + opExt
            String opPath = path.toString() + File.separator + opName
            if (!new File(opPath).exists()) {
                Log.err("The pipeline's 'outputDescriptor'" +
                        " expected '$opName' but the file wasn't found")
                retVal = false
            }

        }

        return retVal

    }

}