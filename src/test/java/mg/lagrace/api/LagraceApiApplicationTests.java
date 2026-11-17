package mg.lagrace.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.cloudinary.Cloudinary;
import mg.lagrace.api.models.BanquetRate;
import mg.lagrace.api.models.BookingCharge;

import mg.lagrace.api.dto.booking.BookingBanquetCreateRequest;
import mg.lagrace.api.dto.booking.BookingChargeUpdateRequest;
import mg.lagrace.api.dto.booking.BookingChargeVoidRequest;
import mg.lagrace.api.dto.booking.BookingChargeCreateRequest;
import mg.lagrace.api.dto.booking.BookingDepositRequest;
import mg.lagrace.api.dto.booking.BookingDepositReturnRequest;
import mg.lagrace.api.dto.booking.BookingCancelRequest;
import mg.lagrace.api.dto.booking.BookingCancellationFee;
import mg.lagrace.api.dto.booking.BookingRoomCancelRequest;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.repositories.BookingChargeRepository;
import mg.lagrace.api.repositories.BanquetRateRepository;
import mg.lagrace.api.repositories.AdditionalServiceRepository;
import mg.lagrace.api.repositories.BookingOtherRepository;
import mg.lagrace.api.repositories.PaymentMethodRepository;
import mg.lagrace.api.repositories.PaymentRepository;
import mg.lagrace.api.models.Space;
import mg.lagrace.api.dto.booking.BookingSpaceUpdateRequest;
import mg.lagrace.api.services.BookingChargeService;
import mg.lagrace.api.services.BookingPaymentService;
import mg.lagrace.api.services.BookingCancellationService;
import mg.lagrace.api.dto.booking.space.BookingSpaceRequest;
import mg.lagrace.api.dto.booking.pool.BookingPoolRequest;
import mg.lagrace.api.dto.booking.meeting.BookingMeetingRoomRequest;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingStatusRepository;
import mg.lagrace.api.repositories.CustomerRepository;
import mg.lagrace.api.repositories.SpaceRepository;
import mg.lagrace.api.services.BookingSpaceService;
import mg.lagrace.api.services.BookingSpaceActionService;
import mg.lagrace.api.services.BookingDetailService;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.repositories.BookingSpaceRepository;
import mg.lagrace.api.repositories.RoomDetailRepository;
import mg.lagrace.api.repositories.RoomRepository;
import mg.lagrace.api.repositories.RoomStatusHistoryRepository;
import mg.lagrace.api.repositories.RoomStatusRepository;
import mg.lagrace.api.repositories.RoomTypeRepository;
import mg.lagrace.api.repositories.PoolRepository;
import mg.lagrace.api.repositories.PoolServiceRepository;
import mg.lagrace.api.repositories.PoolRateRepository;
import mg.lagrace.api.repositories.MeetingRoomRepository;
import mg.lagrace.api.repositories.MeetingRoomServiceRepository;
import mg.lagrace.api.repositories.MeetingRoomRateRepository;
import mg.lagrace.api.repositories.UserRepository;
import mg.lagrace.api.services.RoomService;
import mg.lagrace.api.services.RoomTypeService;
import mg.lagrace.api.services.BookingPoolService;
import mg.lagrace.api.services.BookingMeetingRoomService;

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

    @Autowired
    private BookingSpaceActionService bookingSpaceActionService;

    @Autowired
    private BookingSpaceRepository bookingSpaceRepository;

    @Autowired
    private BookingChargeRepository bookingChargeRepository;

    @Autowired
    private BookingChargeService bookingChargeService;

    @Autowired
    private BookingPaymentService bookingPaymentService;

    @Autowired
    private BookingCancellationService bookingCancellationService;

    @Autowired
    private BanquetRateRepository banquetRateRepository;

    @Autowired
    private AdditionalServiceRepository additionalServiceRepository;

    @Autowired
    private BookingOtherRepository bookingOtherRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingDetailService bookingDetailService;

    @Autowired
    private BookingPoolService bookingPoolService;

    @Autowired
    private BookingMeetingRoomService bookingMeetingRoomService;

    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private PoolServiceRepository poolServiceRepository;

    @Autowired
    private PoolRateRepository poolRateRepository;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Autowired
    private MeetingRoomServiceRepository meetingRoomServiceRepository;

    @Autowired
    private MeetingRoomRateRepository meetingRoomRateRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
            List.of(new BookingSpaceRequest.ExtraItem(12L, null, 1, null)),
            null,
            null,
            null,
            null,
            true,
            null
        );

        var result = bookingSpaceService.createBooking(request, "ADMIN", false);

        assertNotNull(result.ref());
        assertEquals("CONFIRMED", result.statusCode());
        assertEquals(1, result.lines().size());
        assertEquals("SPACE", result.lines().get(0).type());
        assertTrue(result.lines().get(0).main());
        assertNotNull(result.lines().get(0).ref());
        assertTrue(result.balance().chargesTotal().signum() > 0);
        assertEquals(0, result.balance().total().compareTo(result.balance().linesTotal().add(result.balance().chargesTotal())));

        var detail = bookingDetailService.get(result.id());
        assertEquals("SPACE", detail.lines().get(0).type());
        assertEquals(space.getName(), detail.lines().get(0).spaceLabel());
        assertEquals(eventDate, detail.lines().get(0).eventDate());
        assertTrue(detail.lines().get(0).main());
        assertEquals("NOT_COLLECTED", detail.deposit().state());
        assertEquals("BANQUET", detail.charges().get(0).category().name());

        var line = bookingSpaceRepository.findAllByBooking_IdBooking(result.id()).get(0);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test",
            List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));
        var nonAdmin = new UsernamePasswordAuthenticationToken("ADMIN", "test");
        entityManager.clear();
        line = bookingSpaceRepository.findById(line.getIdBookingSpace()).orElseThrow();
        Long spaceLineId = line.getIdBookingSpace();
        Integer confirmedVersion = line.getVersion();
        var offDateFailure = assertThrows(mg.lagrace.api.exceptions.ConflictException.class,
            () -> bookingSpaceActionService.start(spaceLineId,
                new BookingStatusActionRequest(confirmedVersion), nonAdmin));
        assertEquals("FORBIDDEN", offDateFailure.getCode());
        detail = bookingSpaceActionService.start(spaceLineId,
            new BookingStatusActionRequest(confirmedVersion), authentication);
        assertEquals("CHECK_IN", detail.lines().get(0).statusCode());
        assertTrue(detail.lines().get(0).allowed().end());

        entityManager.clear();
        line = bookingSpaceRepository.findById(spaceLineId).orElseThrow();
        detail = bookingSpaceActionService.end(line.getIdBookingSpace(),
            new BookingStatusActionRequest(line.getVersion(), "État des lieux vérifié"), authentication);
        assertEquals("CHECK_OUT", detail.lines().get(0).statusCode());
        assertTrue(!detail.lines().get(0).allowed().end());
        assertTrue(detail.history().stream().anyMatch(history -> "État des lieux vérifié".equals(history.reason())));
    }

        @Test
        @Transactional
        void createPoolBookingReturnsDetailWithoutCollectingPayment() {
        var pool = poolRepository.findAll().stream().findFirst().orElseThrow();
        var entryService = poolServiceRepository.findAll().stream()
            .filter(service -> "ENTRY".equalsIgnoreCase(service.getCode())).findFirst().orElseThrow();
        LocalDate eventDate = LocalDate.now().plusMonths(8);
        var rate = poolRateRepository.findApplicableByPoolAndService(pool.getIdPool(), entryService.getIdService(), eventDate)
            .stream().filter(candidate -> candidate.getMinAge() != null || candidate.getMaxAge() != null)
            .findFirst().orElseThrow();
        var startsAt = eventDate.atTime(10, 0).atZone(java.time.ZoneId.of("Indian/Antananarivo")).toOffsetDateTime();
        BookingPoolRequest request = new BookingPoolRequest(1L, null, null, "pool-create-no-payment-test",
            new BookingPoolRequest.PoolItem(pool.getIdPool(), entryService.getIdService(), startsAt, null,
                2, List.of(new BookingPoolRequest.RateItem(rate.getIdRate(), 2))), null, false, null);

        var detail = bookingPoolService.createBooking(request, "ADMIN");

        assertEquals("PENDING", detail.statusCode());
        assertEquals(1, detail.lines().size());
        assertEquals("POOL", detail.lines().get(0).type());
        assertEquals(2, detail.lines().get(0).quantity());
        assertEquals(0, detail.payments().size());
        assertEquals(pool.getName(), detail.lines().get(0).poolLabel());
        }

        @Test
        @Transactional
        void createMeetingRoomBookingReturnsDetailWithoutCollectingPayment() {
        var meetingRoom = meetingRoomRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate serviceDate = LocalDate.now().plusMonths(8);
        var selected = meetingRoomServiceRepository.findAll().stream()
            .map(service -> new java.util.AbstractMap.SimpleEntry<>(service,
                meetingRoomRateRepository.findApplicableForBooking(meetingRoom.getIdMeetingRoom(), service.getIdService(), serviceDate)))
            .filter(entry -> !entry.getValue().isEmpty()).findFirst().orElseThrow();
        var service = selected.getKey();
        BookingMeetingRoomRequest request = new BookingMeetingRoomRequest(1L, null, null,
            "meeting-create-no-payment-test",
            List.of(new BookingMeetingRoomRequest.MeetingRoomItem(meetingRoom.getIdMeetingRoom(),
                service.getIdService(), serviceDate, null)), null, false, null);

        var detail = bookingMeetingRoomService.createBooking(request, "ADMIN");

        assertEquals("PENDING", detail.statusCode());
        assertEquals(1, detail.lines().size());
        assertEquals("MEETING_ROOM", detail.lines().get(0).type());
        assertEquals(meetingRoom.getName(), detail.lines().get(0).roomLabel());
        assertEquals(service.getName(), detail.lines().get(0).formulaLabel());
        assertEquals(0, detail.payments().size());
        }

    @Test
    @Transactional
    void updateSpaceAllowsPartialFieldChangesAndKeepsExistingValues() {
        Space space = spaceRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate eventDate = LocalDate.now().plusMonths(2);
        BookingSpaceRequest request = new BookingSpaceRequest(
            1L,
            null,
            null,
            "partial-space-update-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, eventDate, 10, true),
            List.of(new BookingSpaceRequest.BanquetItem(1L, 10)),
            null,
            null,
            null,
            null,
            null,
            true,
            null
        );

        var created = bookingSpaceService.createBooking(request, "ADMIN", false);
        var line = bookingSpaceRepository.findAllByBooking_IdBooking(created.id()).get(0);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test",
            List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));

        BookingSpaceUpdateRequest update = new BookingSpaceUpdateRequest(
            line.getVersion(),
            null,
            eventDate.plusDays(1),
            null,
            null,
            null,
            null,
            null,
            true,
            null
        );

        var detail = bookingSpaceService.updateSpace(line.getIdBookingSpace(), update, authentication);
        assertNotNull(detail);
        entityManager.flush();
        entityManager.clear();
        var updatedLine = bookingSpaceRepository.findById(line.getIdBookingSpace()).orElseThrow();
        assertEquals(eventDate.plusDays(1), updatedLine.getEventDate());
        assertEquals(10, updatedLine.getGuestsCount());
        assertTrue(updatedLine.getWithCaterer());
    }

    @Test
    @Transactional
    void banquetChargesRepriceAtTierCrossingAndCannotRemoveTheLastCateredMenu() {
        Space space = spaceRepository.findAll().stream()
            .filter(candidate -> candidate.getName().equals("Grande Salle"))
            .findFirst().orElseGet(() -> spaceRepository.findAll().stream()
                .filter(candidate -> candidate.getCapacity() != null && candidate.getCapacity() >= 100)
                .findFirst().orElseThrow());
        var menuTypes = entityManager.createQuery("SELECT type FROM BanquetMenuType type", mg.lagrace.api.models.BanquetMenuType.class).getResultList();
        var allRates = banquetRateRepository.findAll();
        var tierPair = allRates.stream().filter(rate -> rate.getMaxPersons() != null)
            .flatMap(lower -> allRates.stream().filter(higher -> higher.getMenuType().getIdMenuType().equals(lower.getMenuType().getIdMenuType())
                    && higher.getMinPersons() == lower.getMaxPersons() + 1)
                .map(higher -> {
                    LocalDate start = LocalDate.now().plusDays(1);
                    if (lower.getValidFrom().isAfter(start)) start = lower.getValidFrom();
                    if (higher.getValidFrom().isAfter(start)) start = higher.getValidFrom();
                    return new Object[] { lower, higher, start };
                }))
            .filter(pair -> {
                LocalDate start = (LocalDate) pair[2];
                BanquetRate lower = (BanquetRate) pair[0];
                BanquetRate higher = (BanquetRate) pair[1];
                return (lower.getValidTo() == null || !lower.getValidTo().isBefore(start))
                    && (higher.getValidTo() == null || !higher.getValidTo().isBefore(start));
            }).findFirst().orElseThrow();
        BanquetRate lowerTier = (BanquetRate) tierPair[0];
        BanquetRate higherTier = (BanquetRate) tierPair[1];
        LocalDate eventDate = (LocalDate) tierPair[2];
        var otherMenuType = menuTypes.stream()
            .filter(type -> !type.getIdMenuType().equals(lowerTier.getMenuType().getIdMenuType()))
            .findFirst().orElseThrow();
        int guestCount = Math.max(120, higherTier.getMinPersons());
        BookingSpaceRequest createRequest = new BookingSpaceRequest(
            1L, null, null, "banquet-tier-update-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, eventDate, guestCount, true),
            List.of(new BookingSpaceRequest.BanquetItem(otherMenuType.getIdMenuType(), Math.min(90, guestCount))),
            null, null, null, null, null, true, null);
        var created = bookingSpaceService.createBooking(createRequest, "ADMIN", false);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test");

        bookingChargeService.addBanquet(created.id(), new BookingBanquetCreateRequest(lowerTier.getMenuType().getIdMenuType(), lowerTier.getMaxPersons()), authentication);
        BookingCharge buffetCharge = bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> charge.getBanquetRate() != null
            && charge.getBanquetRate().getMenuType().getIdMenuType().equals(lowerTier.getMenuType().getIdMenuType()))
            .findFirst().orElseThrow();
        assertEquals(lowerTier.getMaxPersons(), buffetCharge.getQuantity());

        BookingChargeUpdateRequest update = new BookingChargeUpdateRequest();
        update.setVersion(buffetCharge.getVersion());
        update.setPersons(higherTier.getMinPersons());
        bookingChargeService.update(buffetCharge.getIdBookingCharge(), update, authentication);
        entityManager.flush();
        entityManager.clear();
        buffetCharge = bookingChargeRepository.findById(buffetCharge.getIdBookingCharge()).orElseThrow();
        assertEquals(higherTier.getMinPersons(), buffetCharge.getQuantity());
        assertEquals(higherTier.getIdRate(), buffetCharge.getBanquetRate().getIdRate());
        assertEquals(0, buffetCharge.getUnitPrice().compareTo(higherTier.getAmount()));

        bookingChargeService.voidCharge(buffetCharge.getIdBookingCharge(),
            new BookingChargeVoidRequest(buffetCharge.getVersion(), null), authentication);
        BookingCharge servedCharge = bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> charge.getBanquetRate() != null
                && charge.getBanquetRate().getMenuType().getIdMenuType().equals(otherMenuType.getIdMenuType()))
            .findFirst().orElseThrow();
        assertThrows(mg.lagrace.api.exceptions.UnprocessableEntityException.class,
            () -> bookingChargeService.voidCharge(servedCharge.getIdBookingCharge(),
                new BookingChargeVoidRequest(servedCharge.getVersion(), null), authentication));
    }

    @Test
    @Transactional
    void spaceServiceUsesCatalogPriceAndEnforcesCatererRules() {
        Space space = spaceRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate eventDate = LocalDate.now().plusMonths(2);
        BookingSpaceRequest createRequest = new BookingSpaceRequest(
            1L, null, null, "space-service-rules-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, eventDate, 10, true),
            List.of(new BookingSpaceRequest.BanquetItem(1L, 10)),
            null, null, null, null, null, true, null);
        var created = bookingSpaceService.createBooking(createRequest, "ADMIN", false);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test");
        var authorized = new UsernamePasswordAuthenticationToken("ADMIN", "test",
            List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));

        AdditionalService freeService = additionalServiceRepository.findAll().stream()
            .filter(service -> service.isActive() && service.isFreeWithCaterer()
                && "SPACE".equalsIgnoreCase(service.getScope().getCode()))
            .findFirst().orElseThrow();
        freeService.setCateringCondition(null);
        additionalServiceRepository.saveAndFlush(freeService);
        bookingChargeService.add(created.id(), new BookingChargeCreateRequest(null, freeService.getIdService(), 1, null), authentication);
        BookingCharge freeCharge = bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> charge.getAdditionalService() != null
                && charge.getAdditionalService().getIdService().equals(freeService.getIdService()))
            .findFirst().orElseThrow();
        assertTrue(freeCharge.getIncluded());
        assertEquals(0, freeCharge.getUnitPrice().compareTo(freeService.getPrice()));

        BookingChargeUpdateRequest makePayable = new BookingChargeUpdateRequest();
        makePayable.setVersion(freeCharge.getVersion());
        makePayable.setIncluded(false);
        assertThrows(mg.lagrace.api.exceptions.BusinessException.class,
            () -> bookingChargeService.update(freeCharge.getIdBookingCharge(), makePayable, authentication));
        bookingChargeService.update(freeCharge.getIdBookingCharge(), makePayable, authorized);
        assertTrue(!bookingChargeRepository.findById(freeCharge.getIdBookingCharge()).orElseThrow().getIncluded());

        AdditionalService incompatible = additionalServiceRepository.findAll().stream()
            .filter(service -> service.isActive()
                && service.getCateringCondition() == mg.lagrace.api.models.enums.CateringCondition.WITHOUT_CATERER
                && "SPACE".equalsIgnoreCase(service.getScope().getCode()))
            .findFirst().orElseThrow();
        var failure = assertThrows(mg.lagrace.api.exceptions.UnprocessableEntityException.class,
            () -> bookingChargeService.add(created.id(), new BookingChargeCreateRequest(null, incompatible.getIdService(), 1, null), authentication));
        assertEquals("CATERER_RULE", failure.getCode());
    }

    @Test
    @Transactional
    void depositCollectionAndWithholdingAreAtomicAndIdempotent() {
        Space space = spaceRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate eventDate = LocalDate.now().plusMonths(6);
        BookingSpaceRequest createRequest = new BookingSpaceRequest(
            1L, null, null, "deposit-idempotency-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, eventDate, 10, false),
            null, null, null, null, null, null, true, null);
        var created = bookingSpaceService.createBooking(createRequest, "ADMIN", false);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test",
            List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));
        Long methodId = paymentMethodRepository.findAll().stream().findFirst().orElseThrow().getIdMethod();

        UUID collectRequestId = UUID.randomUUID();
        BookingDepositRequest collectRequest = new BookingDepositRequest(methodId, collectRequestId, null);
        var collected = bookingPaymentService.collectSpaceDeposit(created.id(), collectRequest, authentication);
        entityManager.flush();
        entityManager.clear();
        var collectedAgain = bookingPaymentService.collectSpaceDeposit(created.id(), collectRequest, authentication);
        assertEquals("HELD", collected.deposit().state());
        assertEquals(0, collected.deposit().held().compareTo(collected.deposit().amount()),
            "held=" + collected.deposit().held() + ", amount=" + collected.deposit().amount() + ", state=" + collected.deposit().state());
        assertEquals(0, collectedAgain.deposit().held().compareTo(collected.deposit().held()),
            "first=" + collected.deposit().held() + ", replay=" + collectedAgain.deposit().held());
        assertEquals(1, paymentRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(payment -> collectRequestId.equals(payment.getRequestId())).count());
        assertEquals(1, bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> Boolean.TRUE.equals(charge.getDeposit()) && charge.getVoidedAt() == null).count());

        var line = bookingSpaceRepository.findAllByBooking_IdBooking(created.id()).get(0);
        Long spaceLineId = line.getIdBookingSpace();
        bookingSpaceActionService.start(spaceLineId,
            new BookingStatusActionRequest(line.getVersion()), authentication);
        entityManager.flush();
        entityManager.clear();
        line = bookingSpaceRepository.findById(spaceLineId).orElseThrow();
        assertEquals("CHECK_IN", line.getStatus().getCode());
        bookingSpaceActionService.end(spaceLineId,
            new BookingStatusActionRequest(line.getVersion()), authentication);
        entityManager.flush();
        entityManager.clear();
        assertEquals("CHECK_OUT", bookingSpaceRepository.findById(spaceLineId).orElseThrow().getStatus().getCode());

        UUID returnRequestId = UUID.randomUUID();
        BookingDepositReturnRequest returnRequest = new BookingDepositReturnRequest(methodId, returnRequestId,
            new BookingDepositReturnRequest.Withholding("Dégât matériel", new java.math.BigDecimal("25000.00")), null);
        var returned = bookingPaymentService.returnSpaceDeposit(created.id(), returnRequest, authentication);
        var returnedAgain = bookingPaymentService.returnSpaceDeposit(created.id(), returnRequest, authentication);
        assertEquals(0, returned.cashToHandBack().compareTo(returned.deposit().amount().subtract(new java.math.BigDecimal("25000.00"))));
        assertEquals(0, returnedAgain.cashToHandBack().compareTo(returned.cashToHandBack()));
        assertEquals("PARTIALLY_RETAINED", returned.deposit().state());
        assertEquals(1, bookingOtherRepository.findAllByBooking_IdBookingOrderByCreatedAtDesc(created.id()).stream()
            .filter(other -> "Dégât matériel".equals(other.getLabel()) && Boolean.FALSE.equals(other.getIsCancellationFee())).count());
        assertEquals(1, paymentRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(payment -> returnRequestId.equals(payment.getRequestId())
                && "DEPOSIT_REFUND".equals(payment.getKind().getCode())).count());
    }

    @Test
    @Transactional
    void requiredDepositConfigurationBlocksSpaceStartUntilCollected() {
        Space space = spaceRepository.findAll().stream().findFirst().orElseThrow();
        BookingSpaceRequest createRequest = new BookingSpaceRequest(
            1L, null, null, "deposit-required-before-start-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, LocalDate.now().plusMonths(6), 10, false),
            null, null, null, null, null, null, true, null);
        var created = bookingSpaceService.createBooking(createRequest, "ADMIN", false);
        var line = bookingSpaceRepository.findAllByBooking_IdBooking(created.id()).get(0);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test",
            List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));
        Object previous = ReflectionTestUtils.getField(bookingSpaceActionService, "depositRequiredBeforeStart");
        ReflectionTestUtils.setField(bookingSpaceActionService, "depositRequiredBeforeStart", true);
        try {
            var failure = assertThrows(mg.lagrace.api.exceptions.ConflictException.class,
                () -> bookingSpaceActionService.start(line.getIdBookingSpace(),
                    new BookingStatusActionRequest(line.getVersion()), authentication));
            assertEquals("FORBIDDEN_TRANSITION", failure.getCode());
        } finally {
            ReflectionTestUtils.setField(bookingSpaceActionService, "depositRequiredBeforeStart", previous);
        }
    }

    @Test
    @Transactional
    void cancelSpaceAndBookingVoidEventChargesButPreserveDepositAndCancellationFees() {
        Space space = spaceRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate eventDate = LocalDate.now().plusMonths(6);
        BookingSpaceRequest createRequest = new BookingSpaceRequest(
            1L, null, null, "space-cancellation-flow-test",
            new BookingSpaceRequest.SpaceItem(space.getIdSpace(), null, eventDate, 10, false),
            null, null, null, null, null, null, true, null);
        var created = bookingSpaceService.createBooking(createRequest, "ADMIN", false);
        var authentication = new UsernamePasswordAuthenticationToken("ADMIN", "test",
            List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));
        AdditionalService spaceService = additionalServiceRepository.findAll().stream()
            .filter(service -> service.isActive() && !service.isDeposit()
                && service.getCateringCondition() == null
                && "SPACE".equalsIgnoreCase(service.getScope().getCode()))
            .findFirst().orElseThrow();
        bookingChargeService.add(created.id(), new BookingChargeCreateRequest(null, spaceService.getIdService(), 1, null), authentication);
        Long methodId = paymentMethodRepository.findAll().stream().findFirst().orElseThrow().getIdMethod();
        bookingPaymentService.collectSpaceDeposit(created.id(), new BookingDepositRequest(methodId, UUID.randomUUID(), null), authentication);

        var spaceLine = bookingSpaceRepository.findAllByBooking_IdBooking(created.id()).get(0);
        var eventCharge = bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> charge.getAdditionalService() != null
                && charge.getAdditionalService().getIdService().equals(spaceService.getIdService()))
            .findFirst().orElseThrow();
        var lineCanceled = bookingCancellationService.cancelSpace(spaceLine.getIdBookingSpace(),
            new BookingRoomCancelRequest(spaceLine.getVersion(), "Événement reporté",
                new BookingCancellationFee("Frais espace", new java.math.BigDecimal("10000.00"))), authentication);
        assertTrue(lineCanceled.bookingHasNoActiveLine());
        assertEquals("CANCELED", bookingSpaceRepository.findById(spaceLine.getIdBookingSpace()).orElseThrow().getStatus().getCode());
        assertEquals("Événement annulé", bookingChargeRepository.findById(eventCharge.getIdBookingCharge()).orElseThrow().getVoidReason());
        assertTrue(bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> Boolean.TRUE.equals(charge.getDeposit())).allMatch(charge -> charge.getVoidedAt() == null));
        assertTrue(lineCanceled.depositToReturn().compareTo(java.math.BigDecimal.ZERO) > 0);

        Booking booking = bookingRepository.findById(created.id()).orElseThrow();
        var bookingCanceled = bookingCancellationService.cancelBooking(created.id(),
            new BookingCancelRequest(booking.getVersion(), "Client annule", 
                new BookingCancellationFee("Frais dossier", new java.math.BigDecimal("5000.00"))), authentication);
        assertEquals("CANCELED", bookingCanceled.statusCode());
        assertEquals(2, bookingOtherRepository.findAllByBooking_IdBookingOrderByCreatedAtDesc(created.id()).stream()
            .filter(other -> Boolean.TRUE.equals(other.getIsCancellationFee()) && other.getVoidedAt() == null).count());
        assertTrue(bookingCanceled.depositToReturn().compareTo(java.math.BigDecimal.ZERO) > 0);
        assertTrue(bookingChargeRepository.findAllByBooking_IdBooking(created.id()).stream()
            .filter(charge -> Boolean.TRUE.equals(charge.getDeposit())).allMatch(charge -> charge.getVoidedAt() != null));
        assertTrue(paymentRepository.findAllByBooking_IdBooking(created.id()).stream()
            .noneMatch(payment -> Set.of("REFUND", "DEPOSIT_REFUND").contains(payment.getKind().getCode())));
    }
}
