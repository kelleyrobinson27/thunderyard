package com.thunderyard.demo.sms;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.thunderyard.demo.config.CorrelationIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DlqWorkerTest {

    private final DlqWorker worker = new DlqWorker();

    @AfterEach
    void clearMdc() {
        CorrelationIds.close();
    }

    @Test
    void logsDlqWithCorrelationMdcThenClears() {
        Logger logger = (Logger) LoggerFactory.getLogger(DlqWorker.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        SmsJob job = SmsJob.builder()
                .notificationId(UUID.randomUUID())
                .correlationId("corr-dlq")
                .build();

        try {
            worker.handleDlq(job);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list.getFirst().getMDCPropertyMap())
                .containsEntry(CorrelationIds.MDC_KEY, "corr-dlq");
        assertThat(CorrelationIds.current()).isNull();
    }
}
