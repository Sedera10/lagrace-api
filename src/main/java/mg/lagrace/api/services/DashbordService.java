package mg.lagrace.api.services;
import java.math.BigDecimal; import java.time.Clock; import java.time.LocalDate; import java.time.ZoneId; import java.util.List;
import mg.lagrace.api.dto.dashboard.DashboardDtos.*; import mg.lagrace.api.repositories.DashboardNativeRepository; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true) public class DashbordService {
 private final DashboardNativeRepository repository; private final ReportingService reporting; private final Clock clock;
 public DashbordService(DashboardNativeRepository repository,ReportingService reporting,Clock clock){this.repository=repository;this.reporting=reporting;this.clock=clock;}
 private LocalDate date(LocalDate value){return value==null?LocalDate.now(clock.withZone(ZoneId.of("Indian/Antananarivo"))):value;}
 public Progress countArrivals(LocalDate d){return repository.arrivals(date(d));} public Progress countDepartures(LocalDate d){return repository.departures(date(d));} public Occupancy occupiedRooms(LocalDate d){return repository.occupied(date(d));} public BigDecimal amountToCollect(){return repository.amountToCollect();} public BigDecimal collectedOn(LocalDate d){return repository.collected(date(d));} public long tasksCount(LocalDate d){return repository.taskCount(date(d));}
 public Cards cards(LocalDate d){d=date(d);return new Cards(countArrivals(d),countDepartures(d),occupiedRooms(d),amountToCollect(),collectedOn(d),tasksCount(d));}
 public List<ArrivalDetail> arrivals(LocalDate d){return repository.arrivalsList(date(d));} public List<DepartureDetail> departures(LocalDate d){return repository.departuresList(date(d));} public List<RoomLive> roomsLive(LocalDate d){return repository.roomsLive(date(d));}
 public List<Event> events(LocalDate d,int days){return List.of();} public Tasks tasks(LocalDate d){TaskGroup none=new TaskGroup(0,List.of());return new Tasks(none,none,none,none,none,none,none,none);} public Cash cash(LocalDate d){return new Cash(List.of(),collectedOn(d));} public List<SpaceLive> spacesLive(LocalDate d){return List.of();} public List<FreeRoom> freeRooms(LocalDate d){return List.of();}
 public Month month(LocalDate d){d=date(d);return new Month(reporting.revenueByActivity(d.withDayOfMonth(1),d.plusDays(1)),reporting.occupancySeries(d.minusDays(13),d));}
}
