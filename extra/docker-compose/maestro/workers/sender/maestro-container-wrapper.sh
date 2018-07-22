#!/bin/bash
echo "Synchronizing time"
ntpd -d -q -n -p pool.ntp.org
MAESTRO_BROKER=${MAESTRO_BROKER:-mqtt://broker:1883}
MAESTRO_LOG_DIR=${MAESTRO_LOG_DIR:-/maestro/sender/logs}

${MAESTRO_APP_ROOT}/maestro-worker/bin/maestro-worker -m ${MAESTRO_BROKER} -w org.maestro.worker.jms.JMSSenderWorker -l ${MAESTRO_LOG_DIR} -r sender
