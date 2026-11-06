package mg.lagrace.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.cloudinary.Cloudinary;

import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.Space;
import mg.lagrace.api.dto.booking.space.BookingSpaceRequest;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingStatusRepository;
import mg.lagrace.api.repositories.CustomerRepository;
import mg.lagrace.api.repositories.SpaceRepository;
import mg.lagrace.api.services.BookingSpaceService;
import mg.lagrace.api.repositories.RoomDetailRepository;
import mg.lagrace.api.repositories.RoomRepository;
import mg.lagrace.api.repositories.RoomStatusHistoryRepository;
import mg.lagrace.api.repositories.RoomStatusRepository;
import mg.lagrace.api.repositories.RoomTypeRepository;
import mg.lagrace.api.services.RoomService;
import mg.lagrace.api.services.RoomTypeService;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class LagraceApiApplicationTests {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingStatusRepository bookingStatusRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private BookingSpaceService bookingSpaceService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomDetailRepository roomDetailRepository;

    @Mock
    private RoomStatusRepository roomStatusRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomStatusHistoryRepository roomStatusHistoryRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private RoomTypeService roomTypeService;

    @Test
    void contextLoads() {
    }

    @Test
    void getAvailableRoomIds_returnsOnlyFreeRooms() {
        RoomService roomService = new RoomService(
            roomRepository,
            roomDetailRepository,
            roomStatusRepository,
            roomTypeRepository,
            cloudinary,
            roomStatusHistoryRepository,
            roomTypeService
        );

        LocalDate checkIn = LocalDate.of(2026, 11, 10);
        LocalDate checkOut = LocalDate.of(2026, 11, 12);

        when(roomRepository.findAvailableRoomIds(checkIn, checkOut)).thenReturn(List.of(1L, 3L));

        assertEquals(List.of(1L, 3L), roomService.getAvailableRoomIds(checkIn, checkOut));
    }

    @Test
    @Transactional
    void triggerGeneratedBookingReferenceSurvivesStatusUpdate() {
        Booking booking = new Booking();
        booking.setCustomer(customerRepository.findById(1L).orElseThrow());
        booking.setStatus(bookingStatusRepository.findByCode("PENDING"));
        booking.setNotes("rollback-test-generated-reference");

        booking = bookingRepository.saveAndFlush(booking);
        String generatedReference = booking.getRef();

        assertNotNull(generatedReference);
        booking.setStatus(bookingStatusRepository.findByCode("CONFIRMED"));
        bookingRepository.saveAndFlush(booking);

        assertEquals(generatedReference, bookingRepository.findById(booking.getIdBooking()).orElseThrow().getRef());
    }

    @Test
    @Transactional
    void createSpaceBookingUsesDatabaseRateAndGeneratedReferences() {
        Space space = spaceRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate eventDate = LocalDate.now().plusMonths(6);
        BookingSpaceRequest request = new BookingSpaceRequest(
            1L,
            null,
            null,
            "rollback-space-booking-integration-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, eventDate, 10, true),
            List.of(new BookingSpaceRequest.BanquetItem(1L, 10)),
            List.of(new BookingSpaceRequest.ExtraItem(12L, null, 1)),
            null,
            null,
            null,
            true,
            null
        );

        var result = bookingSpaceService.createBooking(request, "ADMIN", false);

        assertNotNull(result.reference());
        assertEquals("CONFIRMED", result.statusCode());
        assertEquals(1, result.lines().size());
        assertEquals("SPACE", result.lines().get(0).lineType());
        assertNotNull(result.lines().get(0).reference());
        assertTrue(result.chargesTotal().signum() > 0);
        assertEquals(0, result.total().compareTo(result.linesTotal().add(result.chargesTotal())));
    }
}
