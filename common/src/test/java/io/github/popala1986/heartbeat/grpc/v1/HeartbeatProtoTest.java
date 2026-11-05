package io.github.popala1986.heartbeat.grpc.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

class HeartbeatProtoTest {

    @Test
    void requestSurvivesSerializationRoundTrip() throws InvalidProtocolBufferException {
        HeartbeatRequest original = HeartbeatRequest.newBuilder()
                .setCorrelationId("abc-123")
                .setDeviceId("device-1")
                .setSequence(42)
                .setSentAt(Timestamp.newBuilder().setSeconds(1_700_000_000L).build())
                .build();

        byte[] bytes = original.toByteArray();
        HeartbeatRequest parsed = HeartbeatRequest.parseFrom(bytes);

        assertEquals(original, parsed);
        assertEquals("abc-123", parsed.getCorrelationId());
        assertEquals(42, parsed.getSequence());
    }

    @Test
    void newResponseHasUnspecifiedStatusByDefault() {
        HeartbeatResponse response = HeartbeatResponse.newBuilder().build();

        assertEquals(HeartbeatStatus.HEARTBEAT_STATUS_UNSPECIFIED, response.getStatus());
    }
}
