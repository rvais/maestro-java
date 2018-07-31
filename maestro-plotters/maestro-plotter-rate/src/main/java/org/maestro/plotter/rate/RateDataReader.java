package org.maestro.plotter.rate;

import org.maestro.common.io.data.common.RateEntry;
import org.maestro.common.io.data.readers.BinaryRateReader;
import org.maestro.plotter.common.ReportReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class RateDataReader implements ReportReader<RateData> {
    private static final Logger logger = LoggerFactory.getLogger(RateDataReader.class);
    private RateData rateData = new RateData();

    @Override
    public RateData read(final File file) throws IOException {
        try (BinaryRateReader binaryRateReader = new BinaryRateReader(file)) {
            RateEntry entry = binaryRateReader.readRecord();

            while (entry != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Read record: {}", entry);
                }

                /*
                 * TODO: I think on Java 9 and newer Instant has greater precision.
                 * This needs some checks.
                 *
                 * For reference:
                 * - https://bugs.openjdk.java.net/browse/JDK-8068730
                 * - http://blog.joda.org/2017/02/java-time-jsr-310-enhancements-java-9.html
                 */
                long timestamp = TimeUnit.MICROSECONDS.toMillis(entry.getTimestamp());

                Instant instant = Instant.ofEpochMilli(timestamp);

                if (logger.isTraceEnabled()) {
                    logger.trace("Record timestamp: {} ", instant);
                }

                final RateRecord rateRecord = new RateRecord(instant, entry.getCount());
                rateData.add(rateRecord);

                entry = binaryRateReader.readRecord();
            }

        }
        return rateData;
    }

}
