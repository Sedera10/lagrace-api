package mg.lagrace.api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import mg.lagrace.api.models.views.RoomDetail;
import mg.lagrace.api.repositories.RoomDetailRepository;
import mg.lagrace.api.repositories.RoomRepository;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomDetailRepository roomDetailRepository;

    public RoomService (RoomRepository roomRepository, RoomDetailRepository roomDetailRepository) {
        this.roomRepository = roomRepository;
        this.roomDetailRepository = roomDetailRepository;
    }

    public List<RoomDetail> getRooms() {
        return roomDetailRepository.findAll();
    }
}
