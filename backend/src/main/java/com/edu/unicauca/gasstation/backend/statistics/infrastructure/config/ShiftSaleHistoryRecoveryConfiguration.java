package com.edu.unicauca.gasstation.backend.statistics.infrastructure.config;

import com.edu.unicauca.gasstation.backend.statistics.application.ShiftSaleHistoryRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Explicit polling, enabled by default. Does not rely on Modulith automatic retries or restart replay. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "statistics.shift-sales.recovery", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ShiftSaleHistoryRecoveryConfiguration {

    @Bean
    RecoveryJob shiftSaleHistoryRecoveryJob(ShiftSaleHistoryRecoveryService recovery) {
        return new RecoveryJob(recovery);
    }

    public static class RecoveryJob {
        private static final Logger LOG = LoggerFactory.getLogger(RecoveryJob.class);
        private final ShiftSaleHistoryRecoveryService recovery;

        RecoveryJob(ShiftSaleHistoryRecoveryService recovery) {
            this.recovery = recovery;
        }

        @Scheduled(fixedDelayString = "${statistics.shift-sales.recovery.delay-ms:60000}",
                initialDelayString = "${statistics.shift-sales.recovery.initial-delay-ms:60000}")
        public void run() {
            try {
                recovery.recoverPending();
            } catch (RuntimeException failure) {
                // Database/registry outage: preserve publications and try again on the next scheduled pass.
                LOG.error("Could not recover pending shift sales; the next scheduled pass will retry", failure);
            }
        }
    }
}
