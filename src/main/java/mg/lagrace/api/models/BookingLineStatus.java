package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "booking_line_status")
public class BookingLineStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking_line_status")
    private Long idBookingLineStatus;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    public Long getIdBookingLineStatus() { return idBookingLineStatus; }
    public void setIdBookingLineStatus(Long idBookingLineStatus) { this.idBookingLineStatus = idBookingLineStatus; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
}
