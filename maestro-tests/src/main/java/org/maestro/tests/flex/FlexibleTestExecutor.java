/*
 * Copyright 2018 Otavio R. Piske <angusyoung@gmail.com>
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

package org.maestro.tests.flex;

import org.maestro.client.Maestro;
import org.maestro.reports.downloaders.ReportsDownloader;
import org.maestro.tests.AbstractTestExecutor;
import org.maestro.tests.AbstractTestProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple test executor that can be extended for use with 3rd party testing tools
 */
public abstract class FlexibleTestExecutor extends AbstractTestExecutor {
    private static final Logger logger = LoggerFactory.getLogger(FlexibleTestExecutor.class);
    private final Maestro maestro;

    private final FlexibleTestProcessor testProcessor;
    private final ReportsDownloader reportsDownloader;
    private final AbstractTestProfile testProfile;

    private int notificationRetries = 2;


    /**
     * Constructor
     * @param maestro a Maestro client instance
     * @param reportsDownloader the reports downloader in use for the test
     * @param testProfile the test profile in use for the test
     */
    public FlexibleTestExecutor(final Maestro maestro, final ReportsDownloader reportsDownloader,
                                final AbstractTestProfile testProfile)
    {
        super(maestro, reportsDownloader);

        this.maestro = maestro;
        this.reportsDownloader = reportsDownloader;
        this.testProfile = testProfile;

        testProcessor = new FlexibleTestProcessor(testProfile, reportsDownloader);
    }


    /**
     * These two methods are NO-OP in this case because there are no multiple iterations,
     * therefore cool down period is not required/used
     */
    public long getCoolDownPeriod() {
        return 0;
    }

    public void setCoolDownPeriod(long period) {
        // NO-OP
    }

    abstract public void startServices();

    public void setNotificationRetries(int notificationRetries) {
        this.notificationRetries = 2;
    }

    /**
     * Test execution logic
     * @return true if the test was successful or false otherwise
     */
    public boolean run() {
        try {
            // Clean up the topic
            logger.debug("Cleaning up the topic");
            maestro.collect();

            logger.info("Collecting the number of peers");
            int numPeers = getNumPeers();

            logger.info("Resolving data servers");
            resolveDataServers();
            processReplies(testProcessor, notificationRetries, numPeers);

            getReportsDownloader().getOrganizer().getTracker().setCurrentTest(testProfile.getTestExecutionNumber());

            logger.info("Applying the test profile");
            testProfile.apply(maestro);

            testProcessor.resetNotifications();

            logger.info("Starting the services");
            startServices();

            logger.info("Processing the replies");
            processReplies(testProcessor, notificationRetries, numPeers);

            logger.info("Processing the notifications");
            processNotifications(testProcessor, notificationRetries, numPeers);
        } catch (InterruptedException e) {
            logger.info("Test execution interrupted");
        } finally {
            maestro.stopAgent();
        }

        return testProcessor.isSuccessful();
    }
}
