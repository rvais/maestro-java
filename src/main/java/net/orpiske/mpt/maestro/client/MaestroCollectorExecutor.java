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

package net.orpiske.mpt.maestro.client;

import net.orpiske.mpt.maestro.notes.MaestroNote;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

public class MaestroCollectorExecutor implements Runnable {
    private MaestroCollector maestroCollector = null;
    private volatile boolean exit = false;

    public MaestroCollectorExecutor(final String url) throws MqttException {
        maestroCollector = new MaestroCollector(url);

        System.out.println("Connecting the collector");
        maestroCollector.connect();

        System.out.println("Subscribing the collector");
        maestroCollector.subscribe();
    }

    public void run() {
        while (!exit) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        try {
            maestroCollector.disconnect();
        } catch (MqttException e) {
            // e.printStackTrace();
        }

        exit = true;
    }

    public List<MaestroNote> collect() {
        return maestroCollector.collect();
    }
}