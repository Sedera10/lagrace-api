package mg.lagrace.api.leave;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LeaveRequestTest {
    @Test
    void initializesCreatedAtBeforePersistence() {
        LeaveRequest request = new LeaveRequest();

        request.onCreate();

        assertNotNull(request.getCreatedAt());
    }
}
