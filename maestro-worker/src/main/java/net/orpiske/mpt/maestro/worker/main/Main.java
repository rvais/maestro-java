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

package net.orpiske.mpt.maestro.worker.main;

import net.orpiske.mpt.common.exceptions.MaestroConnectionException;
import net.orpiske.mpt.common.Constants;
import net.orpiske.mpt.common.LogConfigurator;
import net.orpiske.mpt.common.exceptions.MaestroException;
import net.orpiske.mpt.common.worker.MaestroDriver;
import net.orpiske.mpt.common.worker.MaestroWorker;
import org.apache.commons.cli.*;


public class Main {
    private static CommandLine cmdLine;
    private static Options options;

    private static String maestroUrl;
    private static String worker;
    private static String role;
    private static String host;

    /**
     * Prints the help for the action and exit
     * @param options the options object
     * @param code the exit code
     */
    private static void help(final Options options, int code) {
        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp(Constants.BIN_NAME, options);
        System.exit(code);
    }

    private static void processCommand(String[] args) {
        CommandLineParser parser = new PosixParser();

        options = new Options();

        options.addOption("h", "help", false, "prints the help");
        options.addOption("m", "maestro-url", true,
                "maestro URL to connect to");
        options.addOption("w", "worker", true,
                "maestro worker to use");
        options.addOption("r", "role", true,
                "worker role (sender or receiver)");
        options.addOption("H", "host", true,
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
            help(options, -1);
        }

        worker = cmdLine.getOptionValue('w');
        if (worker == null) {
            help(options, -1);
        }

        role = cmdLine.getOptionValue('r');
        if (role == null) {
            help(options, -1);
        }

        host = cmdLine.getOptionValue('H');
        if (host == null) {
            help(options, -1);
        }

    }

    public static void main(String[] args) {
        processCommand(args);

        LogConfigurator.trace();

        try {

            Class<MaestroWorker> clazz = (Class<MaestroWorker>) Class.forName(worker);

            MaestroWorker w = clazz.newInstance();

            MaestroWorkerExecutor executor = new MaestroWorkerExecutor(maestroUrl, role, host, w);

            executor.run();
            System.out.println("Finished execution ...");

        } catch (MaestroException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
