package mg.lagrace.api.models;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "calendar_day")
public class CalendarDay {
    @Id
    @Column(name = "day")
    private LocalDate day;
    @Column(name = "is_holiday", nullable = false)
    private boolean holiday;

    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }
    public boolean isHoliday() { return holiday; }
    public void setHoliday(boolean holiday) { this.holiday = holiday; }
}
