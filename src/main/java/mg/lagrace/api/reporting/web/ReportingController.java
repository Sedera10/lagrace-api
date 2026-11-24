package mg.lagrace.api.reporting.web;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos;

import java.time.LocalDate;
import java.util.List;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos.*;
import mg.lagrace.api.reporting.application.ReportingService;
import mg.lagrace.api.reporting.application.ReportingExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reporting")
public class ReportingController {
	private final ReportingService service;
	private final ReportingExportService exports;

	public ReportingController(ReportingService service, ReportingExportService exports) {
		this.service = service;
		this.exports = exports;
	}

	@GetMapping("/export/{report}")
	public ResponseEntity<byte[]> export(@org.springframework.web.bind.annotation.PathVariable String report,
			@RequestParam String format, @RequestParam LocalDate from, @RequestParam LocalDate to,
			@RequestParam(required = false) String status, @RequestParam(required = false) String origin,
			@RequestParam(name = "service", required = false) String serviceType,
			@RequestParam(required = false) Long customerId, Authentication authentication) {
		requireFinance(authentication);
		if (!"bookings".equals(report)) throw new IllegalArgumentException("Rapport d’export inconnu.");
		if (!"csv".equals(format) && !"pdf".equals(format)) throw new IllegalArgumentException("format doit être csv ou pdf.");
		byte[] bytes = exports.exportBookings(format, from, to, status, origin, serviceType, customerId);
		String filename = "rapport-" + report + "-" + from + "-" + to + "." + format;
		MediaType media = "csv".equals(format) ? MediaType.parseMediaType("text/csv;charset=UTF-8") : MediaType.APPLICATION_PDF;
		return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.attachment().filename(filename).build().toString()).body(bytes);
	}

	private void requireFinance(Authentication authentication) {
		if (authentication == null || authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().equals("ROLE_DROIT_FINANCES"))) {
			throw new AccessDeniedException("DROIT_FINANCES requis");
		}
	}

	@GetMapping("/revenue-by-activity")
	public ApiResponse<ActivityReport> revenueByActivity(@RequestParam LocalDate from,
			@RequestParam LocalDate to, @RequestParam(required = false) String groupBy,
			@RequestParam(defaultValue = "false") boolean compare, Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(service.revenueByActivity(from, to, groupBy, compare));
	}

	@GetMapping("/realized-revenue-by-activity")
	public ApiResponse<List<Activity>> legacyRevenueByActivity(@RequestParam LocalDate from,
			@RequestParam LocalDate to, Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(service.realizedRevenueByActivity(from, to));
	}

	@GetMapping("/revenue-by-month")
	public ApiResponse<RevenueByMonth> revenueByMonth(@RequestParam List<Integer> years,
			@RequestParam(defaultValue = "false") boolean compare, Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(service.revenueByMonth(years, compare));
	}

	@GetMapping("/occupancy")
	public ApiResponse<OccupancyReport> occupancy(@RequestParam LocalDate from, @RequestParam LocalDate to,
			@RequestParam(required = false) String groupBy, @RequestParam(defaultValue = "false") boolean compare) {
		return ApiResponse.success(service.occupancy(from, to, groupBy, compare));
	}

	@GetMapping("/occupancy-rate")
	public ApiResponse<Rate> occupancyRate(@RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.success(new Rate(service.occupancyRate(from, to)));
	}

	@GetMapping("/cancellation")
	public ApiResponse<CancellationReport> cancellation(@RequestParam LocalDate from, @RequestParam LocalDate to,
			@RequestParam(required = false) String groupBy, @RequestParam(defaultValue = "false") boolean compare) {
		return ApiResponse.success(service.cancellation(from, to, groupBy, compare));
	}

	@GetMapping("/bookings-count")
	public ApiResponse<Series> bookingsCount(@RequestParam LocalDate from, @RequestParam LocalDate to,
			@RequestParam(required = false) String groupBy,
			@RequestParam(defaultValue = "false") boolean compare) {
		return ApiResponse.success(service.bookingsCountSeries(from, to, groupBy, compare));
	}

	@GetMapping("/cancellation-rate")
	public ApiResponse<Rate> cancellationRate(@RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.success(new Rate(service.cancellationRate(from, to)));
	}

	@GetMapping("/booking-rate-by-room-type")
	public ApiResponse<List<RoomTypeRate>> bookingRateByRoomType(@RequestParam LocalDate from,
			@RequestParam LocalDate to, @RequestParam(required = false) String groupBy,
			@RequestParam(defaultValue = "false") boolean compare) {
		return ApiResponse.success(service.bookingRateByRoomType(from, to, groupBy, compare));
	}

	@GetMapping("/average-stay")
	public ApiResponse<AverageStayReport> averageStay(@RequestParam LocalDate from, @RequestParam LocalDate to,
			@RequestParam(required = false) String groupBy, @RequestParam(defaultValue = "false") boolean compare) {
		return ApiResponse.success(service.averageStay(from, to, groupBy, compare));
	}

	@GetMapping("/bookings")
	public ApiResponse<BookingsReport> bookings(@RequestParam LocalDate from, @RequestParam LocalDate to,
			@RequestParam(required = false) String status, @RequestParam(required = false) String origin,
			@RequestParam(name = "service", required = false) String serviceType,
			@RequestParam(required = false) Long customerId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(service.bookingsReport(from, to, status, origin, serviceType, customerId, page, size, sort));
	}

	@GetMapping("/realized-revenue")
	public ApiResponse<Money> revenue(@RequestParam LocalDate from, @RequestParam LocalDate to, Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(new Money(service.realizedRevenue(from, to)));
	}

	@GetMapping("/invoiced")
	public ApiResponse<Money> invoiced(@RequestParam LocalDate from, @RequestParam LocalDate to, Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(new Money(service.invoiced(from, to)));
	}

	@GetMapping("/collected")
	public ApiResponse<Money> collected(@RequestParam LocalDate from, @RequestParam LocalDate to, Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(new Money(service.collected(from, to)));
	}

	@GetMapping("/outstanding")
	public ApiResponse<Money> outstanding(Authentication authentication) {
		requireFinance(authentication);
		return ApiResponse.success(new Money(service.outstanding()));
	}
}
