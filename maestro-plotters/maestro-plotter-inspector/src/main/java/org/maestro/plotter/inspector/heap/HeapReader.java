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

package org.maestro.plotter.inspector.heap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.maestro.plotter.common.readers.StreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;

public class HeapReader extends StreamReader<HeapData> {
    private static final Logger logger = LoggerFactory.getLogger(HeapReader.class);

    private final HeapProcessor heapProcessor;

    public HeapReader() {
        this.heapProcessor = new HeapProcessor();
    }

    public HeapReader(HeapProcessor heapProcessor) {
        this.heapProcessor = heapProcessor;
    }

    @Override
    protected HeapData readReader(Reader reader) throws IOException {
        Iterable<CSVRecord> records = CSVFormat.RFC4180
                .withCommentMarker('#')
                .withFirstRecordAsHeader()
                .withRecordSeparator(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.NON_NUMERIC)
                .parse(reader);



        for (CSVRecord record : records) {
            try {
                heapProcessor.process(record.get(0), record.get(1), record.get(2), record.get(3), record.get(4),
                        record.get(5));
            } catch (Throwable t) {
                logger.warn("Unable to parse record: {}", t.getMessage(), t);
                continue;
            }
        }

        return heapProcessor.getHeapData();
    }
}
