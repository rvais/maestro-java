package net.orpiske.mpt.main.actions;

import net.orpiske.mpt.reports.ReportGenerator;
import net.orpiske.mpt.reports.processors.DiskCleaner;
import org.apache.commons.cli.*;

public class ReportAction extends Action {
    private CommandLine cmdLine;
    private Options options;

    private String directory;
    private boolean clean;

    public ReportAction(String[] args) {
        processCommand(args);
    }

    protected void processCommand(String[] args) {
        CommandLineParser parser = new PosixParser();

        options = new Options();

        options.addOption("h", "help", false, "prints the help");
        options.addOption("d", "directory", true, "the directory to generate the report");
        options.addOption("l", "log-level", true, "the log level to use [trace, debug, info, warn]");
        options.addOption("C", "clean", false, "clean the report directory after processing");

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            help(options, -1);
        }

        if (cmdLine.hasOption("help")) {
            help(options, 0);
        }

        directory = cmdLine.getOptionValue('d');
        if (directory == null) {
            System.err.println("The input directory is a required option");
            help(options, 1);
        }

        String logLevel = cmdLine.getOptionValue('l');
        configureLogLevel(logLevel);

        clean = cmdLine.hasOption('C');
    }

    public int run() {
        try {
            ReportGenerator reportGenerator = new ReportGenerator(directory);

            if (clean) {
                reportGenerator.getPostProcessors().add(new DiskCleaner());
            }

            reportGenerator.generate();
            return 0;
        }
        catch (Exception e) {
            System.err.println("Unable to generate the performance test reports");
            e.printStackTrace();
            return 1;
        }
    }
}
