package org.maestro.plotter.amqp.inspector.memory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.maestro.plotter.common.readers.StreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;

/**
 * A csv router link data reader
 */
public class QDMemoryReader extends StreamReader<QDMemoryDataSet> {
    private static final Logger logger = LoggerFactory.getLogger(QDMemoryReader.class);

    private final QDMemoryProcessor qdMemoryProcessor;

    public QDMemoryReader() {
        this.qdMemoryProcessor = new QDMemoryProcessor();
    }

    public QDMemoryReader(QDMemoryProcessor qdMemoryProcessor) {
        this.qdMemoryProcessor = qdMemoryProcessor;
    }

    /**
     * Reader of csv file
     * @param reader reader
     * @return readed data
     * @throws IOException implementation specific
     */
    @Override
    protected QDMemoryDataSet readReader(Reader reader) throws IOException {
        Iterable<CSVRecord> records = CSVFormat.RFC4180
                .withCommentMarker('#')
                .withFirstRecordAsHeader()
                .withRecordSeparator(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.NON_NUMERIC)
                .parse(reader);



        for (CSVRecord record : records) {
            try {
                qdMemoryProcessor.process(record.get(0), record.get(1), record.get(2), record.get(3), record.get(4),
                        record.get(5), record.get(6), record.get(7), record.get(8), record.get(9), record.get(10));
            } catch (Throwable t) {
                logger.warn("Unable to parse record: {}", t.getMessage(), t);
                continue;
            }
        }

        return qdMemoryProcessor.getQDMemoryDataSet();
    }
}
