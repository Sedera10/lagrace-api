package mg.lagrace.api.controllers;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomResponse;
import mg.lagrace.api.dto.booking.room.BookingResponse;
import mg.lagrace.api.dto.booking.space.BookingSpaceRequest;
import mg.lagrace.api.dto.booking.pool.BookingPoolRequest;
import mg.lagrace.api.dto.booking.meeting.BookingMeetingRoomRequest;
import mg.lagrace.api.services.BookingRoomService;
import mg.lagrace.api.services.BookingSpaceService;
import mg.lagrace.api.services.BookingPoolService;
import mg.lagrace.api.services.BookingMeetingRoomService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingUpdateRequest;
import mg.lagrace.api.dto.booking.BookingChargeCreateRequest;
import mg.lagrace.api.dto.booking.BookingBanquetCreateRequest;
import mg.lagrace.api.services.BookingDetailService;
import mg.lagrace.api.services.BookingUpdateService;
import mg.lagrace.api.services.BookingChargeService;
import mg.lagrace.api.services.BookingCancellationService;
import mg.lagrace.api.dto.booking.BookingCancelRequest;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.dto.booking.BookingPaymentRequest;
import mg.lagrace.api.dto.booking.BookingDepositRequest;
import mg.lagrace.api.dto.booking.BookingDepositReturnRequest;
import mg.lagrace.api.services.BookingLifecycleService;
import mg.lagrace.api.services.BookingChangesService;
import mg.lagrace.api.services.BookingPaymentService;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingRoomService bookingRoomService;
    private final BookingSpaceService bookingSpaceService;
    private final BookingPoolService bookingPoolService;
    private final BookingMeetingRoomService bookingMeetingRoomService;
    private final BookingDetailService bookingDetailService;
    private final BookingUpdateService bookingUpdateService;
    private final BookingChargeService bookingChargeService;
    private final BookingCancellationService bookingCancellationService;
    private final BookingLifecycleService bookingLifecycleService;
    private final BookingChangesService bookingChangesService;
    private final BookingPaymentService bookingPaymentService;

    public BookingController(BookingRoomService bookingRoomService, BookingSpaceService bookingSpaceService,
            BookingPoolService bookingPoolService, BookingMeetingRoomService bookingMeetingRoomService,
            BookingDetailService bookingDetailService, BookingUpdateService bookingUpdateService,
            BookingChargeService bookingChargeService, BookingCancellationService bookingCancellationService,
            BookingLifecycleService bookingLifecycleService, BookingChangesService bookingChangesService,
            BookingPaymentService bookingPaymentService) {
        this.bookingRoomService = bookingRoomService;
        this.bookingSpaceService = bookingSpaceService;
        this.bookingPoolService = bookingPoolService;
        this.bookingMeetingRoomService = bookingMeetingRoomService;
        this.bookingDetailService = bookingDetailService;
        this.bookingUpdateService = bookingUpdateService;
        this.bookingChargeService = bookingChargeService;
        this.bookingCancellationService = bookingCancellationService;
        this.bookingLifecycleService = bookingLifecycleService;
        this.bookingChangesService = bookingChangesService;
        this.bookingPaymentService = bookingPaymentService;
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<BookingRoomResponse>> createBookingRoom(
            @Valid @RequestBody BookingRoomRequest request) {
        BookingRoomResponse booking = bookingRoomService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Réservation créée avec succès."));
    }

    @PostMapping({"/space", "/spaces"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBookingSpace(
        @Valid @RequestBody BookingSpaceRequest request,
        Authentication authentication) {
    boolean canOffer = authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_Administrateur".equals(authority.getAuthority()));
    BookingDetailResponse booking = bookingSpaceService.createBooking(request, authentication.getName(), canOffer);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(booking, "Réservation d'espace créée avec succès."));
    }

    @PostMapping({"/pool", "/pools"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBookingPool(
        @Valid @RequestBody BookingPoolRequest request,
        Authentication authentication) {
    BookingDetailResponse booking = bookingPoolService.createBooking(request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(booking, "Réservation de piscine créée avec succès."));
    }

    @PostMapping("/meeting-rooms")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBookingMeetingRoom(
        @Valid @RequestBody BookingMeetingRoomRequest request,
        Authentication authentication) {
    BookingDetailResponse booking = bookingMeetingRoomService.createBooking(request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(booking, "Réservation de salle de réunion créée avec succès."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> listBookings(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingRoomService.listBookings(customerId, page, size)));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(bookingDetailService.get(bookingId)));
    }

    @GetMapping("/{bookingId}/invoices")
    public ResponseEntity<ApiResponse<BookingDetailResponse.Invoicing>> getInvoices(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(bookingDetailService.getInvoicing(bookingId)));
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> updateBooking(
            @PathVariable Long bookingId, @Valid @RequestBody BookingUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(bookingUpdateService.update(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
            @PathVariable Long bookingId, @Valid @RequestBody BookingCancelRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingCancellationService.cancelBooking(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> confirmBooking(
        @PathVariable Long bookingId, @Valid @RequestBody BookingStatusActionRequest request,
        Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
        bookingLifecycleService.confirm(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/close")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> closeBooking(
        @PathVariable Long bookingId, @Valid @RequestBody BookingStatusActionRequest request,
        Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
        bookingLifecycleService.close(bookingId, request, authentication)));
    }

    @PatchMapping("/{bookingId}/changes")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> applyChanges(
            @PathVariable Long bookingId, @RequestBody JsonNode request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingChangesService.apply(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/charges")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> addCharge(@PathVariable Long bookingId,
            @Valid @RequestBody BookingChargeCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(bookingChargeService.add(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/banquet")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> addBanquet(@PathVariable Long bookingId,
            @Valid @RequestBody BookingBanquetCreateRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                bookingChargeService.addBanquet(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> addPayment(@PathVariable Long bookingId,
            @Valid @RequestBody BookingPaymentRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(bookingPaymentService.pay(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/deposit/collect")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> collectSpaceDeposit(@PathVariable Long bookingId,
            @Valid @RequestBody BookingDepositRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingPaymentService.collectSpaceDeposit(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/deposit/return")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> returnSpaceDeposit(@PathVariable Long bookingId,
            @Valid @RequestBody BookingDepositReturnRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
            bookingPaymentService.returnSpaceDeposit(bookingId, request, authentication)));
    }

    @GetMapping("/by-ref/{ref}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingByRef(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.success(bookingDetailService.getByRef(ref)));
    }
}
