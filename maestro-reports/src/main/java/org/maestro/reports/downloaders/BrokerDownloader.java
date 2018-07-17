package org.maestro.reports.downloaders;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.maestro.client.callback.MaestroNoteCallback;
import org.maestro.client.exchange.MaestroTopics;
import org.maestro.client.notes.LocationType;
import org.maestro.client.notes.LogResponse;
import org.maestro.common.ConfigurationWrapper;
import org.maestro.common.HostTypes;
import org.maestro.common.NodeUtils;
import org.maestro.common.URLUtils;
import org.maestro.common.client.notes.MaestroNote;
import org.maestro.contrib.utils.digest.Sha1Digest;
import org.maestro.reports.ReceiverReportResolver;
import org.maestro.reports.ReportResolver;
import org.maestro.reports.SenderReportResolver;
import org.maestro.reports.organizer.NodeOrganizer;
import org.maestro.reports.organizer.Organizer;
import org.maestro.client.Maestro;
import org.maestro.reports.organizer.ResultStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BrokerDownloader implements ReportsDownloader {
    private static final Logger logger = LoggerFactory.getLogger(BrokerDownloader.class);

    private static class DownloadCallback implements MaestroNoteCallback {
        private static final Logger logger = LoggerFactory.getLogger(DownloadCallback.class);
        private final NodeOrganizer organizer;
        private final Sha1Digest digest = new Sha1Digest();
        private long lastDownloadTime = 0;

        DownloadCallback(final NodeOrganizer organizer) {
            this.organizer = organizer;
        }

        private void save(final LogResponse logResponse) {
            switch (logResponse.getLocationType()) {
                case LAST_SUCCESS: {
                    organizer.setResultType(ResultStrings.SUCCESS);
                    break;
                }
                default: {
                    organizer.setResultType(ResultStrings.FAILED);
                    break;
                }
            }

            String type = NodeUtils.getTypeFromName(logResponse.getName());
            String destDir = organizer.organize(logResponse.getName(), type);
            File outFile = new File(destDir, logResponse.getFileName());

            logger.info("Saving file {} to {}", logResponse.getFileName(), outFile);
            if (!outFile.exists()) {
                try {
                    FileUtils.forceMkdirParent(outFile);
                } catch (IOException e) {
                    logger.error("Unable to create parent directories: {}", e.getMessage(), e);
                }
            }

            try (FileOutputStream fo = new FileOutputStream(outFile)) {
                IOUtils.copy(logResponse.getLogData(), fo);
            } catch (FileNotFoundException e) {
                logger.error("Unable to save the file: {}", e.getMessage(), e);
            } catch (IOException e) {
                logger.error("Unable to save the file due to I/O error: {}", e.getMessage(), e);
            }

            verify(logResponse, outFile);
        }

        private void verify(LogResponse logResponse, File outFile) {
            if (logResponse.getFileHash() != null && !logResponse.getFileHash().isEmpty()) {
                try {
                    logger.info("Verifying SHA-1 hash for file {}", outFile);
                    if (!digest.verify(outFile.getPath(), logResponse.getFileHash())) {
                        logger.error("The SHA-1 hash for file {} does not match the expected one {}",
                                outFile.getPath(), logResponse.getFileHash());
                    }
                } catch (IOException e) {
                    logger.error("Unable to verify the hash for file {}: {}", outFile.getName(),
                            e.getMessage());
                }
            }
            else {
                logger.warn("The peer did not set up a hash for file {}", logResponse.getFileName());
            }
        }

        @Override
        public void call(MaestroNote note) {
            if (note instanceof LogResponse) {
                LogResponse logResponse = (LogResponse) note;
                if (logResponse.getLocationType() == LocationType.LAST_FAILED) {
                    logger.info("About to download last failed reports");
                }
                else {
                    logger.info("About to download last successful reports");
                }
                try {
                    save(logResponse);
                }
                finally {
                    lastDownloadTime = System.currentTimeMillis();
                }
            }
        }

        public long getLastDownloadTime() {
            return lastDownloadTime;
        }
    }

    private static final AbstractConfiguration config = ConfigurationWrapper.getConfig();
    private final Map<String, ReportResolver> resolverMap = new HashMap<>();
    private final Maestro maestro;

    private final NodeOrganizer organizer;
    private final DownloadCallback downloadCallback;

    public BrokerDownloader(final Maestro maestro, final String baseDir) {
        this(maestro, new NodeOrganizer(baseDir));
    }

    // For the builder. TODO: improve
    BrokerDownloader(final Maestro maestro, final Organizer organizer) {
        this(maestro, (NodeOrganizer) organizer);
    }

    public BrokerDownloader(final Maestro maestro, final NodeOrganizer organizer) {
        this.maestro = maestro;
        this.organizer = organizer;

        resolverMap.put(HostTypes.SENDER_HOST_TYPE, new SenderReportResolver());
        resolverMap.put(HostTypes.RECEIVER_HOST_TYPE, new ReceiverReportResolver());


        maestro.getCollector().subscribe(MaestroTopics.MAESTRO_LOGS_TOPIC, 0);

        downloadCallback = new DownloadCallback(organizer);
        maestro.getCollector().getCallbacks().add(downloadCallback);
    }

    @Override
    public Organizer getOrganizer() {
        return organizer;
    }

    @Override
    public void addReportResolver(final String hostType, final ReportResolver reportResolver) {
        resolverMap.put(hostType, reportResolver);
    }

    private String getDataServerHost(final String dataServer) throws MalformedURLException {
        final URL url = new URL(dataServer);

        return url.getHost();
    }


    private void download(final String type, final String dataServer, LocationType locationType) {
        final String host = URLUtils.getHostnameFromURL(dataServer);
        final String topic = MaestroTopics.peerTopic(type, host);

        maestro.logRequest(topic, locationType , null);
    }

    @Override
    public void downloadLastSuccessful(final String type, final String dataServer) {
        download(type, dataServer, LocationType.LAST_SUCCESS);
    }


    @Override
    public void downloadLastFailed(final String type, final String dataServer) {
        download(type, dataServer, LocationType.LAST_FAILED);
    }

    @Override
    public void downloadAny(final String type, final String dataServer, final String testNumber) {
        try {
            final String host = getDataServerHost(dataServer);
            final String topic = MaestroTopics.peerTopic(type, host);

            maestro.logRequest(topic, LocationType.ANY , testNumber);
        } catch (MalformedURLException e) {
            logger.error("Unable to parse data server URL {}: {}", dataServer, e.getMessage());
        }
    }

    private Instant lastDownloadTime() {
        return Instant.ofEpochMilli(downloadCallback.getLastDownloadTime());
    }

    @Override
    public void waitForComplete() {
        int expiryTime = config.getInt("download.broker.expiry", 60);

        logger.info("Waiting {} seconds until all the files have been downloaded from the broker", expiryTime);
        Instant last = lastDownloadTime();


        while (last.plusSeconds(expiryTime).isAfter(Instant.now())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ok
                break;
            }
        }
    }
}
