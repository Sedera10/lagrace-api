package mg.lagrace.api.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import mg.lagrace.api.dto.dashboard.DashboardDtos.BookingReportRow;
import mg.lagrace.api.dto.dashboard.DashboardDtos.BookingsReport;
import mg.lagrace.api.models.HotelSettings;
import mg.lagrace.api.repositories.HotelSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportingExportService {
  private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private final ReportingService reports; private final HotelSettingsRepository hotels;
  public ReportingExportService(ReportingService reports,HotelSettingsRepository hotels){this.reports=reports;this.hotels=hotels;}
  public byte[] exportBookings(String format,LocalDate from,LocalDate to,String status,String origin,String service,Long customerId){
    BookingsReport report=reports.bookingsExport(from,to,status,origin,service,customerId);
    return "csv".equals(format)?csv(report):pdf(report,from,to);
  }
  private byte[] csv(BookingsReport report){StringBuilder out=new StringBuilder("\uFEFFRéférence;Client;Origine;Statut;Début;Fin;Services;Total;Payé;Solde\r\n");for(BookingReportRow r:report.items())out.append(cell(r.reference())).append(';').append(cell(r.customer())).append(';').append(cell(r.origin())).append(';').append(cell(r.status())).append(';').append(date(r.periodStart())).append(';').append(date(r.periodEnd())).append(';').append(cell(r.services())).append(';').append(amount(r.total())).append(';').append(amount(r.paid())).append(';').append(amount(r.balance())).append("\r\n");out.append("Totaux;;;;;;;"+amount(report.totals().total())+";"+amount(report.totals().paid())+";"+amount(report.totals().balance())+"\r\n");return out.toString().getBytes(StandardCharsets.UTF_8);}
  private byte[] pdf(BookingsReport report,LocalDate from,LocalDate to){try{ByteArrayOutputStream out=new ByteArrayOutputStream();Document doc=new Document(PageSize.A4.rotate(),36,36,42,36);PdfWriter.getInstance(doc,out);doc.open();HotelSettings hotel=hotels.findById(1L).orElse(null);Font title=new Font(Font.HELVETICA,15,Font.BOLD);doc.add(new Paragraph(hotel==null?"La Grâce":hotel.getLegalName(),title));doc.add(new Paragraph("Rapport des réservations"));doc.add(new Paragraph("Période : "+date(from)+" au "+date(to)+" · Généré le "+date(LocalDate.now())));doc.add(Chunk.NEWLINE);PdfPTable table=new PdfPTable(new float[]{1.1f,1.7f,1f,1f,1f,1f,1.4f,1f,1f,1f});table.setWidthPercentage(100);for(String h:List.of("Référence","Client","Origine","Statut","Début","Fin","Services","Total","Payé","Solde")){PdfPCell c=new PdfPCell(new Phrase(h));c.setBackgroundColor(java.awt.Color.LIGHT_GRAY);table.addCell(c);}for(BookingReportRow r:report.items()){for(String v:List.of(r.reference(),r.customer(),r.origin(),r.status(),date(r.periodStart()),date(r.periodEnd()),r.services(),amount(r.total()),amount(r.paid()),amount(r.balance())))table.addCell(v==null?"":v);}table.addCell("Totaux");for(int i=0;i<6;i++)table.addCell("");table.addCell(amount(report.totals().total()));table.addCell(amount(report.totals().paid()));table.addCell(amount(report.totals().balance()));doc.add(table);doc.add(new Paragraph("Page 1"));doc.close();return out.toByteArray();}catch(DocumentException ex){throw new IllegalStateException("Impossible de générer le PDF",ex);}}
  private String date(LocalDate value){return value==null?"":DATE.format(value);} private String amount(BigDecimal value){return value==null?"0":value.toPlainString();} private String cell(String value){if(value==null)return "";return value.contains(";")||value.contains("\"")?"\""+value.replace("\"","\"\"")+"\"":value;}
}
