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

package org.maestro.agent.base;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.maestro.client.exchange.MaestroTopics;
import org.maestro.client.notes.*;
import org.maestro.client.notes.InternalError;
import org.maestro.common.ConfigurationWrapper;
import org.maestro.common.Constants;
import org.maestro.common.client.exceptions.MalformedNoteException;
import org.maestro.common.client.notes.MaestroNote;
import org.maestro.common.exceptions.MaestroConnectionException;
import org.maestro.common.exceptions.MaestroException;
import org.maestro.worker.common.MaestroWorkerManager;
import org.maestro.worker.common.ds.MaestroDataServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Agent for handle extension points. It implements everything that there is because it servers as a scriptable
 * extension that can act based on any maestro command
 */
public class MaestroAgent extends MaestroWorkerManager implements MaestroAgentEventListener, MaestroSenderEventListener,
        MaestroReceiverEventListener, MaestroInspectorEventListener
{

    private static final Logger logger = LoggerFactory.getLogger(MaestroAgent.class);
    private final GroovyHandler groovyHandler;
    private final AbstractConfiguration config = ConfigurationWrapper.getConfig();
    private final List<ExtensionPoint> extensionPoints = new LinkedList<>();
    private Thread thread;

    private final String sourceRoot;


    /**
     * Constructor
     * @param maestroURL maestro_broker URL
     * @param role agent
     * @param host host address
     * @param dataServer data server object
     * @throws MaestroException if unable to create agent instance
     */
    public MaestroAgent(String maestroURL, String role, String host, MaestroDataServer dataServer) throws MaestroException {
        super(maestroURL, role, host, dataServer);

        String pathStr = config.getString("maestro.agent.ext.path.override", null);

        if (pathStr == null){
            pathStr = Constants.HOME_DIR + "ext" + File.separator + "requests";
        }

        File defaultExtPointFile = new File(pathStr);
        if (defaultExtPointFile.exists()) {
            extensionPoints.add(new ExtensionPoint(defaultExtPointFile, false));
        }
        else  {
            logger.warn("The extension point at {} does not exist", defaultExtPointFile.getPath());
        }

        String defaultSourceDir = FileUtils.getTempDirectoryPath() + File.separator + "maestro-agent-work";

        sourceRoot = config.getString("maestro.agent.source.root", defaultSourceDir);
        groovyHandler = new GroovyHandler(super.getClient());
    }

    /**
     * Start inspector handler
     * @param note StartInspector note
     */
    @Override
    public void handle(StartInspector note) {
        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.START_INSPECTOR, note));
    }

    /**
     * Start receiver handler
     * @param note StartReceiver note
     */
    @Override
    public void handle(StartReceiver note) {
        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.START_RECEIVER, note));
    }

    /**
     * Start sender handler
     * @param note StartSender note
     */
    @Override
    public void handle(StartSender note) {
        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.START_SENDER, note));
    }

    /**
     * Stop Inspector handler
     * @param note StopInspector note
     */
    @Override
    public void handle(StopInspector note) {
        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.STOP_INSPECTOR, note));
    }

    /**
     * Stop receiver handler
     * @param note StopReceiver note
     */
    @Override
    public void handle(StopReceiver note) {
        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.STOP_RECEIVER, note));
    }

    /**
     * Stop sender handler
     * @param note StopSender note
     */
    public void handle(StopSender note) {
        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.STOP_SENDER, note));
    }

    /**
     * Stats request handler
     * @param note Stats note
     */
    @Override
    public void handle(StatsRequest note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.STATS, note));
    }

    /**
     * Flush request handler
     * @param note Flush note
     */
    @Override
    public void handle(FlushRequest note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.FLUSH, note));
    }

    /**
     * Halt request handler
     * @param note Halt note
     */
    @Override
    public void handle(Halt note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.HALT, note));
    }

    /**
     * Set request handler
     * @param note Set note
     */
    @Override
    public void handle(SetRequest note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.SET, note));
    }

    /**
     * Test failed notification handler
     * @param note NotifyFail note
     */
    @Override
    public void handle(TestFailedNotification note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.NOTIFY_FAIL, note));
    }

    /**
     * Test success notification handler
     * @param note NotifySuccess note
     */
    @Override
    public void handle(TestSuccessfulNotification note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.NOTIFY_SUCCESS, note));
    }

    /**
     * Abnormal disconnection handler
     * @param note AbnormalDisconnect note
     */
    @Override
    public void handle(AbnormalDisconnect note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.ABNORMAL_DISCONNECT, note));
    }

    /**
     * Ping request handler
     * @param note Ping note
     * @throws MaestroConnectionException if host is unreachable
     * @throws MalformedNoteException if note is malformed
     */
    @Override
    public void handle(PingRequest note) throws MaestroConnectionException, MalformedNoteException {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.PING, note));
    }

    /**
     * Get request handler
     * @param note Get note
     */
    @Override
    public void handle(GetRequest note) {
        super.handle(note);

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(), AgentConstants.GET, note));
    }


    /**
     * Callbacks wrapper for execute external points scripts
     * @param entryPointPath the root directory of the extension points
     */
    private void callbacksWrapper(final File entryPointPath, final String codeDir, final MaestroNote note) {
        try {
            File entryPointDir = new File(entryPointPath, codeDir);

            groovyHandler.setInitialPath(entryPointDir);
            groovyHandler.setWorkerOptions(getWorkerOptions());
            groovyHandler.setMaestroNote(note);
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        if (logger.isTraceEnabled()) {
                            logger.trace("Executing groovyHandler on thread: {}", Thread.currentThread().getId());
                        }

                        groovyHandler.runCallbacks();

                    }
                    catch (Exception e) {
                        groovyHandler.getClient().notifyFailure(e.getMessage());
                    }
                }
            });

            thread.start();

            this.getClient().replyOk();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error during callback execution: {}", e.getMessage(), e);
            this.getClient().publish(MaestroTopics.MAESTRO_TOPIC, new InternalError());
        }
    }

    /**
     * Start agent handler
     * @param note Start Agent note
     */
    @Override
    public void handle(StartAgent note) {

    }

    private void cleanExtensionPoints(final ExtensionPoint extensionPoint) {
        logger.info("Removing extension point {}", extensionPoint);

        try {
            /*
             The directory comes with the sub-directory "request", as set on
             the SourceRequest handler. Therefore we pick the parent.
            */
            File transientDir = extensionPoint.getPath().getParentFile();
            if (transientDir.exists()) {
                FileUtils.deleteDirectory(transientDir);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop agent handler
     * @param note Stop Agent note
     */
    @Override
    public void handle(StopAgent note) {
        extensionPoints.stream().filter(ep -> ep.isTransient()).forEach(this::cleanExtensionPoints);
        extensionPoints.removeIf(ep -> ep.isTransient());
    }

    // @TODO jstejska: move this into agent somehow?
    @Override
    public void handle(UserCommand1Request note) {
        logger.info("User command request arrived");

        extensionPoints.forEach(point -> callbacksWrapper(point.getPath(),  AgentConstants.USER_COMMAND_1, note));
    }

    @Override
    public void handle(AgentSourceRequest note) {
        logger.info("Source request arrived");

        final String sourceUrl = note.getSourceUrl();
        final String branch = note.getBranch();

        if (branch == null) {
            logger.info("Preparing to download code from {}", sourceUrl);
        }
        else {
            logger.info("Preparing to download code from {} from branch {}", sourceUrl, branch);
        }
        final String projectDir = UUID.randomUUID().toString();

        File repositoryDir = new File(sourceRoot + File.separator + projectDir + File.separator);

        if (!repositoryDir.exists()) {
            if (!repositoryDir.mkdirs()) {
                logger.warn("Unable to create directory: {}", repositoryDir);
            }
        }

        CloneCommand cloneCommand = Git.cloneRepository();

        cloneCommand.setURI(sourceUrl);
        cloneCommand.setDirectory(repositoryDir);
        cloneCommand.setProgressMonitor(NullProgressMonitor.INSTANCE);


        if (branch != null) {
            cloneCommand.setBranch(branch);
        }

        try {
            cloneCommand.call();
            logger.info("Source directory for project created at {}", repositoryDir);
            extensionPoints.add(new ExtensionPoint(new File(repositoryDir, "requests"), true));

            getClient().replyOk();
        } catch (GitAPIException e) {
            logger.error("Unable to clone repository: {}", e.getMessage(), e);
            getClient().replyInternalError();
        }
    }

    @Override
    public void handle(final LogRequest note) {
        final String logDir = System.getProperty("maestro.log.dir");

        // It might or might not exist. The agent is very loose in this regard
        File logDirFile = new File(logDir);
        if (logDirFile.exists()) {
            super.handle(note, logDirFile);
        }
        else {
            logger.warn("The log directory for the agent does not exist");
        }
    }
}
