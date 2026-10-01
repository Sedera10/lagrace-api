package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import mg.lagrace.api.dto.meeting.MeetingRoomRateRequest;
import mg.lagrace.api.dto.meeting.MeetingRoomRateResponse;
import mg.lagrace.api.dto.meeting.MeetingRoomRequest;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.MeetingRoom;
import mg.lagrace.api.models.MeetingRoomRate;
import mg.lagrace.api.models.MeetingRoomService;
import mg.lagrace.api.models.Unit;
import mg.lagrace.api.repositories.MeetingRoomRateRepository;
import mg.lagrace.api.repositories.MeetingRoomRepository;
import mg.lagrace.api.repositories.MeetingRoomServiceRepository;
import mg.lagrace.api.repositories.UnitRepository;

@Service
public class MeetingRoomManagementService {
    private final MeetingRoomRepository rooms;
    private final MeetingRoomRateRepository rates;
    private final MeetingRoomServiceRepository services;
    private final UnitRepository units;
        private final JdbcTemplate jdbcTemplate;

    public MeetingRoomManagementService(MeetingRoomRepository rooms, MeetingRoomRateRepository rates,
            MeetingRoomServiceRepository services, UnitRepository units, JdbcTemplate jdbcTemplate) {
        this.rooms = rooms;
        this.rates = rates;
        this.services = services;
        this.units = units;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MeetingRoom> listRooms() { return rooms.findAll(); }

    @Transactional(readOnly = true)
    public List<MeetingRoomRateResponse> listRates() {
        return rates.findAllByOrderByValidFromDesc().stream().map(this::toResponse).toList();
    }

    public MeetingRoom createRoom(MeetingRoomRequest request) {
        validateRoom(request);
        if (rooms.existsByName(request.name().trim())) throw new ValidationException("Cette salle existe déjà.");
        MeetingRoom room = new MeetingRoom();
        apply(room, request);
        return rooms.save(room);
    }

    public MeetingRoom updateRoom(Long id, MeetingRoomRequest request) {
        validateRoom(request);
        MeetingRoom room = rooms.findById(id).orElseThrow(() -> missing("Salle de réunion", id));
        if (!room.getName().equals(request.name().trim()) && rooms.existsByName(request.name().trim())) {
            throw new ValidationException("Cette salle existe déjà.");
        }
        apply(room, request);
        return rooms.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        rooms.findById(id).orElseThrow(() -> missing("Salle de réunion", id));

        if (rates.existsByMeetingRoom_IdMeetingRoom(id)) {
            throw new ValidationException(
                    "Cette salle ne peut pas être supprimée car elle possède encore des tarifs. Supprimez d'abord ses tarifs.");
        }
        if (hasReservations(id)) {
            throw new ValidationException(
                    "Cette salle ne peut pas être supprimée car elle est liée à une ou plusieurs réservations.");
        }

        rooms.deleteById(id);
    }

        private boolean hasReservations(Long roomId) {
        Boolean tableExists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name = 'reservation_meeting_room')",
            Boolean.class);

        if (!Boolean.TRUE.equals(tableExists)) return false;

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM reservation_meeting_room WHERE meeting_room_id = ?",
            Integer.class,
            roomId);
        return count != null && count > 0;
        }

    @Transactional
    public MeetingRoomRateResponse createRate(MeetingRoomRateRequest request) {
        validateRate(request);
        MeetingRoomRate rate = new MeetingRoomRate();
        apply(rate, request);
        return toResponse(rates.save(rate));
    }

    @Transactional
    public MeetingRoomRateResponse updateRate(Long id, MeetingRoomRateRequest request) {
        validateRate(request);
        MeetingRoomRate rate = rates.findById(id).orElseThrow(() -> missing("Tarif de salle de réunion", id));
        apply(rate, request);
        return toResponse(rates.save(rate));
    }

    public void deleteRate(Long id) { rates.delete(rates.findById(id).orElseThrow(() -> missing("Tarif de salle de réunion", id))); }

    private void apply(MeetingRoom room, MeetingRoomRequest request) {
        room.setName(request.name().trim());
        room.setCapacity(request.capacity());
    }

    private void apply(MeetingRoomRate rate, MeetingRoomRateRequest request) {
        rate.setMeetingRoom(rooms.findById(request.meetingRoomId()).orElseThrow(() -> missing("Salle de réunion", request.meetingRoomId())));
        rate.setService(services.findById(request.serviceId()).orElseThrow(() -> missing("Service de réunion", request.serviceId())));
        rate.setUnit(units.findById(request.unitId()).orElseThrow(() -> missing("Unité", request.unitId())));
        rate.setAmount(request.amount());
        rate.setValidFrom(request.validFrom());
        rate.setValidTo(request.validTo());
    }

    private MeetingRoomRateResponse toResponse(MeetingRoomRate rate) {
        return new MeetingRoomRateResponse(rate.getIdRate(), rate.getMeetingRoom().getIdMeetingRoom(), rate.getService().getIdService(),
                rate.getService().getCode(), rate.getService().getName(), rate.getUnit().getIdUnit(),
                rate.getUnit().getName(), rate.getAmount(), rate.getValidFrom(), rate.getValidTo(), rate.getCreatedAt());
    }

    private void validateRoom(MeetingRoomRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) throw new ValidationException("Le nom de la salle est obligatoire.");
        if (request.capacity() != null && request.capacity() < 1) throw new ValidationException("La capacité doit être positive.");
    }

    private void validateRate(MeetingRoomRateRequest request) {
        if (request == null || request.meetingRoomId() == null || request.serviceId() == null || request.unitId() == null) throw new ValidationException("La salle, le service et l'unité sont obligatoires.");
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Le montant doit être positif ou nul.");
        if (request.validFrom() == null) throw new ValidationException("La date de début est obligatoire.");
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) throw new ValidationException("La date de fin est invalide.");
        rooms.findById(request.meetingRoomId()).orElseThrow(() -> missing("Salle de réunion", request.meetingRoomId()));
    }

    private ResourceNotFoundException missing(String label, Long id) { return new ResourceNotFoundException(label + " introuvable avec l'id " + id); }
}
