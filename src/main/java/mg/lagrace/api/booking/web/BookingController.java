package mg.lagrace.api.booking.web;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.room.BookingRoomRequest;
import mg.lagrace.api.booking.dto.room.BookingRoomResponse;
import mg.lagrace.api.booking.dto.room.BookingResponse;
import mg.lagrace.api.booking.dto.space.BookingSpaceRequest;
import mg.lagrace.api.booking.dto.pool.BookingPoolRequest;
import mg.lagrace.api.booking.dto.meeting.BookingMeetingRoomRequest;
import mg.lagrace.api.booking.application.BookingRoomService;
import mg.lagrace.api.booking.application.BookingSpaceService;
import mg.lagrace.api.booking.application.BookingPoolService;
import mg.lagrace.api.booking.application.BookingMeetingRoomService;
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
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingUpdateRequest;
import mg.lagrace.api.booking.dto.BookingChargeCreateRequest;
import mg.lagrace.api.booking.dto.BookingBanquetCreateRequest;
import mg.lagrace.api.booking.application.BookingDetailService;
import mg.lagrace.api.booking.application.BookingUpdateService;
import mg.lagrace.api.booking.application.BookingChargeService;
import mg.lagrace.api.booking.application.BookingCancellationService;
import mg.lagrace.api.booking.dto.BookingCancelRequest;
import mg.lagrace.api.booking.dto.BookingStatusActionRequest;
import mg.lagrace.api.booking.dto.BookingPaymentRequest;
import mg.lagrace.api.booking.dto.BookingDepositRequest;
import mg.lagrace.api.booking.dto.BookingDepositReturnRequest;
import mg.lagrace.api.booking.application.BookingLifecycleService;
import mg.lagrace.api.booking.application.BookingChangesService;
import mg.lagrace.api.booking.application.BookingPaymentService;
import org.springframework.web.bind.annotation.PatchMapping;
import mg.lagrace.api.auth.application.RequiresPermissions;
import mg.lagrace.api.auth.application.BookingPermissionService;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    @Autowired private BookingPermissionService bookingPermissionService;
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
    @RequiresPermissions({"BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingRoomResponse>> createBookingRoom(
            @Valid @RequestBody BookingRoomRequest request, Authentication authentication) {
        bookingPermissionService.require(authentication, "BOOKING_UPDATE");
        BookingRoomResponse booking = bookingRoomService.createBooking(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Réservation créée avec succès."));
    }

    @PostMapping({"/space", "/spaces"})
    @RequiresPermissions({"BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBookingSpace(
        @Valid @RequestBody BookingSpaceRequest request,
        Authentication authentication) {
    BookingDetailResponse booking = bookingSpaceService.createBooking(request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(booking, "Réservation d'espace créée avec succès."));
    }

    @PostMapping({"/pool", "/pools"})
    @RequiresPermissions({"BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBookingPool(
        @Valid @RequestBody BookingPoolRequest request,
        Authentication authentication) {
    BookingDetailResponse booking = bookingPoolService.createBooking(request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(booking, "Réservation de piscine créée avec succès."));
    }

    @PostMapping("/meeting-rooms")
    @RequiresPermissions({"BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBookingMeetingRoom(
        @Valid @RequestBody BookingMeetingRoomRequest request,
        Authentication authentication) {
    BookingDetailResponse booking = bookingMeetingRoomService.createBooking(request, authentication.getName());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(booking, "Réservation de salle de réunion créée avec succès."));
    }

    @GetMapping
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> listBookings(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingRoomService.listBookings(customerId, page, size)));
    }

    @GetMapping("/{bookingId}")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(bookingDetailService.get(bookingId)));
    }

    @GetMapping("/{bookingId}/invoices")
    @RequiresPermissions({"BOOKING_READ", "INVOICE_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse.Invoicing>> getInvoices(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(bookingDetailService.getInvoicing(bookingId)));
    }

    @PatchMapping("/{bookingId}")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> updateBooking(
            @PathVariable Long bookingId, @Valid @RequestBody BookingUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(bookingUpdateService.update(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/cancel")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_CANCEL"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
            @PathVariable Long bookingId, @Valid @RequestBody BookingCancelRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingCancellationService.cancelBooking(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/confirm")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_CONFIRM"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> confirmBooking(
        @PathVariable Long bookingId, @Valid @RequestBody BookingStatusActionRequest request,
        Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
        bookingLifecycleService.confirm(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/close")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_CLOSE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> closeBooking(
        @PathVariable Long bookingId, @Valid @RequestBody BookingStatusActionRequest request,
        Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
        bookingLifecycleService.close(bookingId, request, authentication)));
    }

    @PatchMapping("/{bookingId}/changes")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> applyChanges(
            @PathVariable Long bookingId, @RequestBody JsonNode request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingChangesService.apply(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/charges")
    @RequiresPermissions({"BOOKING_READ", "CHARGE_ADD"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> addCharge(@PathVariable Long bookingId,
            @Valid @RequestBody BookingChargeCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(bookingChargeService.add(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/banquet")
    @RequiresPermissions({"BOOKING_READ", "CHARGE_ADD"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> addBanquet(@PathVariable Long bookingId,
            @Valid @RequestBody BookingBanquetCreateRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                bookingChargeService.addBanquet(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/payments")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> addPayment(@PathVariable Long bookingId,
            @Valid @RequestBody BookingPaymentRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(bookingPaymentService.pay(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/deposit/collect")
    @RequiresPermissions({"BOOKING_READ", "DEPOSIT_COLLECT"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> collectSpaceDeposit(@PathVariable Long bookingId,
            @Valid @RequestBody BookingDepositRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingPaymentService.collectSpaceDeposit(bookingId, request, authentication)));
    }

    @PostMapping("/{bookingId}/deposit/return")
    @RequiresPermissions({"BOOKING_READ", "DEPOSIT_REFUND"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> returnSpaceDeposit(@PathVariable Long bookingId,
            @Valid @RequestBody BookingDepositReturnRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
            bookingPaymentService.returnSpaceDeposit(bookingId, request, authentication)));
    }

    @GetMapping("/by-ref/{ref}")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingByRef(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.success(bookingDetailService.getByRef(ref)));
    }
}
