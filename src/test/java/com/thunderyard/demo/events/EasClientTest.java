package com.thunderyard.demo.events;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class EasClientTest {

    @Test
    void fallbackIsUsedOnFailure() {
        EasClient client = new EasClient("http://localhost:8081", false);

        UUID id = UUID.randomUUID();

        CompletableFuture<AppointmentDetails> result =
                client.fallbackAppointment(id, new RuntimeException("boom"));

        AppointmentDetails details = result.join();

        assert !details.canNotify();
        assert details.appointmentId().equals(id);
    }

    @Test
    void stubReturnsOptInWithoutCallingHttp() {
        EasClient client = new EasClient("http://localhost:8081", true);
        UUID id = UUID.randomUUID();

        AppointmentDetails details = client.getAppointment(id).join();

        assert details.canNotify();
        assert details.appointmentId().equals(id);
    }

    @Test
    void stubOptOutAppointmentIdCannotNotify() {
        EasClient client = new EasClient("http://localhost:8081", true);

        AppointmentDetails details = client.getAppointment(EasClient.OPT_OUT_APPOINTMENT_ID).join();

        assert !details.canNotify();
        assert details.appointmentId().equals(EasClient.OPT_OUT_APPOINTMENT_ID);
    }
}
