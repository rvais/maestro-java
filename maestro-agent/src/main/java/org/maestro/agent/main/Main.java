/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.maestro.agent.main;

import org.apache.commons.cli.*;
import org.maestro.agent.base.MaestroAgent;
import org.maestro.client.exchange.AbstractMaestroPeer;
import org.maestro.client.exchange.MaestroTopics;
import org.maestro.common.ConfigurationWrapper;
import org.maestro.common.Constants;
import org.maestro.common.LogConfigurator;
import org.maestro.common.exceptions.MaestroException;
import org.maestro.worker.base.VoidWorkerManager;
import org.maestro.worker.ds.MaestroDataServer;
import org.maestro.worker.main.MaestroWorkerExecutor;

import java.io.File;


public class Main {
    private static CommandLine cmdLine;

    private static String maestroUrl;
    private static String host;
    private static File logDir;

    /**
     * Prints the help for the action and exit
     * @param options the options object
     * @param code the exit code
     */
    private static void help(final Options options, int code) {
        HelpFormatter formatter = new HelpFormatter();

        System.out.println("maestro " + Constants.VERSION + "\n");
        formatter.printHelp(Constants.BIN_NAME, options);
        System.exit(code);
    }

    private static void processCommand(String[] args) {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption("h", "help", false, "prints the help");
        options.addOption("m", "maestro-url", true,
                "maestro URL to connect to");
        options.addOption("H", "host", true,
                "this' host hostname");
        options.addOption("l", "log-dir", true,
                "this' host hostname");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            help(options, -1);
        }

        if (cmdLine.hasOption("help")) {
            help(options, 0);
        }

        maestroUrl = cmdLine.getOptionValue('m');
        if (maestroUrl == null) {
            System.err.println("Maestro URL is missing (option -m)");
            help(options, -1);
        }


        host = cmdLine.getOptionValue('H');
        if (host == null) {
            System.err.println("The peer hostname is missing (option -H)");
            help(options, -1);
        }


        String logDirVal = cmdLine.getOptionValue('l');
        if (logDirVal == null) {
            System.err.println("The log directory is missing (option -l)");

            help(options, -1);
        }
        logDir = new File(logDirVal);
    }

    /**
     * Running this as a debug is something like:
     * java -m mqtt://maestro-broker:1883
     *      -H localhost
     *      -l /storage/tmp/maestro-java/sender
     */
    public static void main(String[] args) {
        processCommand(args);

        LogConfigurator.defaultForDaemons();
        try {
            ConfigurationWrapper.initConfiguration(Constants.MAESTRO_CONFIG_DIR, "maestro-agent.properties");
        } catch (Exception e) {
            System.err.println("Unable to initialize configuration file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        try {
            MaestroDataServer dataServer = new MaestroDataServer(logDir);

            MaestroWorkerExecutor executor;
            AbstractMaestroPeer maestroPeer;

            maestroPeer = new MaestroAgent(maestroUrl, "agent", host, dataServer);
            executor = new MaestroWorkerExecutor(maestroPeer, dataServer);

            executor.start(new String[]{MaestroTopics.ALL_DAEMONS});
            executor.run();


            System.out.println("Finished execution ...");
        } catch (MaestroException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        System.exit(0);
    }
}
