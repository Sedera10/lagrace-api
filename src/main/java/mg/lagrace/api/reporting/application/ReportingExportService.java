package mg.lagrace.api.reporting.application;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos.BookingReportRow;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos.BookingsReport;
import mg.lagrace.api.reference.HotelSettings;
import mg.lagrace.api.reference.system.persistence.HotelSettingsRepository;

@Service
public class ReportingExportService {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportingService reports;
    private final HotelSettingsRepository hotels;

    public ReportingExportService(
            ReportingService reports,
            HotelSettingsRepository hotels
    ) {
        this.reports = reports;
        this.hotels = hotels;
    }

    public byte[] exportBookings(
            String format,
            LocalDate from,
            LocalDate to,
            String status,
            String origin,
            String service,
            Long customerId
    ) {
        BookingsReport report = reports.bookingsExport(
                from,
                to,
                status,
                origin,
                service,
                customerId
        );

        return "csv".equals(format)
                ? csv(report)
                : pdf(report, from, to);
    }

    private byte[] csv(BookingsReport report) {
        StringBuilder output = new StringBuilder(
                "\uFEFFRéférence;Client;Origine;Statut;Début;Fin;Services;Total;Payé;Solde\r\n"
        );

        for (BookingReportRow row : report.items()) {
            output.append(cell(row.reference()))
                    .append(';')
                    .append(cell(row.customer()))
                    .append(';')
                    .append(cell(row.origin()))
                    .append(';')
                    .append(cell(row.status()))
                    .append(';')
                    .append(date(row.periodStart()))
                    .append(';')
                    .append(date(row.periodEnd()))
                    .append(';')
                    .append(cell(row.services()))
                    .append(';')
                    .append(amount(row.total()))
                    .append(';')
                    .append(amount(row.paid()))
                    .append(';')
                    .append(amount(row.balance()))
                    .append("\r\n");
        }

        output.append("Totaux;;;;;;;")
                .append(amount(report.totals().total()))
                .append(';')
                .append(amount(report.totals().paid()))
                .append(';')
                .append(amount(report.totals().balance()))
                .append("\r\n");

        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(BookingsReport report, LocalDate from, LocalDate to) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 42, 36);
            PdfWriter.getInstance(document, output);
            document.open();

            HotelSettings hotel = hotels.findById(1L).orElse(null);
            Font title = new Font(Font.HELVETICA, 15, Font.BOLD);
            document.add(new Paragraph(
                    hotel == null ? "La Grâce" : hotel.getLegalName(),
                    title
            ));
            document.add(new Paragraph("Rapport des réservations"));
            document.add(new Paragraph(
                    "Période : " + date(from) + " au " + date(to)
                            + " · Généré le " + date(LocalDate.now())
            ));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(
                    new float[] { 1.1f, 1.7f, 1f, 1f, 1f, 1f, 1.4f, 1f, 1f, 1f }
            );
            table.setWidthPercentage(100);

            for (String heading : List.of(
                    "Référence",
                    "Client",
                    "Origine",
                    "Statut",
                    "Début",
                    "Fin",
                    "Services",
                    "Total",
                    "Payé",
                    "Solde"
            )) {
                PdfPCell cell = new PdfPCell(new Phrase(heading));
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (BookingReportRow row : report.items()) {
                String[] values = {
                        row.reference(),
                        row.customer(),
                        row.origin(),
                        row.status(),
                        date(row.periodStart()),
                        date(row.periodEnd()),
                        row.services(),
                        amount(row.total()),
                        amount(row.paid()),
                        amount(row.balance())
                };
                for (String value : values) {
                    table.addCell(value == null ? "" : value);
                }
            }

            table.addCell("Totaux");
            for (int column = 0; column < 6; column++) {
                table.addCell("");
            }
            table.addCell(amount(report.totals().total()));
            table.addCell(amount(report.totals().paid()));
            table.addCell(amount(report.totals().balance()));

            document.add(table);
            document.add(new Paragraph("Page 1"));
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Impossible de générer le PDF", exception);
        }
    }

    private String date(LocalDate value) {
        return value == null ? "" : DATE_FORMAT.format(value);
    }

    private String amount(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private String cell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
