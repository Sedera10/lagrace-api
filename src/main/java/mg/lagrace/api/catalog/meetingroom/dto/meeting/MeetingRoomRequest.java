package mg.lagrace.api.catalog.meetingroom.dto.meeting;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;

public record MeetingRoomRequest(String name, Integer capacity) {}
