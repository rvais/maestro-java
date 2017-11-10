package net.orpiske.mpt.exporter.main;

import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import net.orpiske.mpt.common.exceptions.MaestroConnectionException;
import net.orpiske.mpt.common.exceptions.MaestroException;
import net.orpiske.mpt.exporter.collectors.ConnectionCount;
import net.orpiske.mpt.exporter.collectors.MessageCount;
import net.orpiske.mpt.exporter.collectors.PingInfo;
import net.orpiske.mpt.exporter.collectors.RateCount;
import net.orpiske.mpt.maestro.Maestro;
import net.orpiske.mpt.maestro.notes.*;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class MaestroExporter {
    private static final Logger logger = LoggerFactory.getLogger(MaestroExporter.class);

    private static final MessageCount messageCounter;
    private static final RateCount rateCounter;
    private static final ConnectionCount connectionCounter;

    private static final PingInfo pingInfo;

    private static final Counter failures;
    private static final Counter successes;
    private static final Counter abnormal;

    private boolean running = true;
    private Maestro maestro = null;

    static {
        messageCounter = MessageCount.getInstance();
        rateCounter = RateCount.getInstance();
        connectionCounter = ConnectionCount.getInstance();
        pingInfo = PingInfo.getInstance();


        failures = Counter.build()
                 .name("maestro_test_failures")
                 .help("Test failures")
                 .register();

        successes = Counter.build().name("maestro_test_success")
                .help("Test success")
                .register();

        abnormal = Counter.build().name("maestro_peer_abnormal_disconnect")
                .help("Abnormal disconnect count")
                .register();

    }

    public MaestroExporter(final String maestroUrl) throws MaestroException {
        maestro = new Maestro(maestroUrl);

        messageCounter.register();
        rateCounter.register();
        connectionCounter.register();
        pingInfo.register();
    }

    private void processNotes(List<MaestroNote> notes) {


        for (MaestroNote note : notes) {
            if (note instanceof StatsResponse) {
                StatsResponse statsResponse = (StatsResponse) note;

                rateCounter.record(statsResponse);
                messageCounter.record(statsResponse);
                connectionCounter.record(statsResponse);
            }
            else {
                if (note instanceof PingResponse) {
                    PingResponse pingResponse = (PingResponse) note;

                    pingInfo.record(pingResponse);
                }
                else {
                   if (note instanceof TestFailedNotification) {
                       failures.inc();
                   }
                   else {
                       if (note instanceof TestSuccessfulNotification) {
                           successes.inc();
                       }
                       else {
                           if (note instanceof AbnormalDisconnect) {
                               abnormal.inc();
                           }
                       }
                   }
                }
            }

            logger.trace("Note: {}", note.toString());
        }
    }


    public int run(int port) throws MaestroConnectionException, IOException {
        logger.info("Exporting metrics on 0.0.0.0:" + port);

        HTTPServer server = null;

        try {
            server = new HTTPServer(port);


            while (running) {
                logger.debug("Sending requests");
                maestro.statsRequest();
                maestro.pingRequest();

                List<MaestroNote> notes = maestro.collect(1000, 5);

                if (notes != null) {
                    processNotes(notes);
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            if (server != null) {
                server.stop();
            }
        }

        return 0;
    }
}
