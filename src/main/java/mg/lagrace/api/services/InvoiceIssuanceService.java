package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.dto.invoice.*;
import mg.lagrace.api.exceptions.*;
import mg.lagrace.api.models.*;
import mg.lagrace.api.repositories.*;

@Service
public class InvoiceIssuanceService {
 private final BillingReadService read; private final BookingRepository bookings; private final InvoiceRepository invoices; private final InvoiceLineRepository lines; private final InvoiceStatusRepository statuses; private final HotelSettingsRepository settings; private final BookingOtherRepository others; private final BookingStatusService bookingStatuses; private final Clock clock;
 public InvoiceIssuanceService(BillingReadService read, BookingRepository bookings, InvoiceRepository invoices, InvoiceLineRepository lines, InvoiceStatusRepository statuses, HotelSettingsRepository settings, BookingOtherRepository others, BookingStatusService bookingStatuses, Clock clock) {this.read=read;this.bookings=bookings;this.invoices=invoices;this.lines=lines;this.statuses=statuses;this.settings=settings;this.others=others;this.bookingStatuses=bookingStatuses;this.clock=clock;}
 @Transactional(readOnly=true) public BillableItemsResponse draft(Long bookingId) { BillableItemsResponse draft=read.billableItems(bookingId); if(draft.items().isEmpty()&&draft.discountItems().isEmpty()) throw new ConflictException("NOTHING_TO_INVOICE","Aucun élément non facturé à inclure dans la facture."); return draft; }
 @Transactional(readOnly=true) public InvoiceDetailResponse preview(InvoiceIssueRequest request) { return build(request, false); }
 @Transactional public InvoiceDetailResponse issue(InvoiceIssueRequest request) { return build(request, true); }
 @Transactional public BillableItemsResponse addDiscount(Long bookingId, DiscountRequest request) {
  Booking booking=bookings.findByIdForUpdate(bookingId).orElseThrow(()->new ResourceNotFoundException("Dossier introuvable avec l'id : "+bookingId)); bookingStatuses.assertBookingOpen(booking);
  BillableItemsResponse draft=read.billableItems(bookingId); BigDecimal positive=draft.items().stream().map(BillableItemsResponse.Item::amount).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal discounts=draft.discountItems().stream().map(x->x.amount().abs()).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(request.amount().compareTo(positive.subtract(discounts))>0) throw new ValidationException("La remise ne peut pas dépasser le total positif non facturé.");
  BookingOther other=new BookingOther(); other.setBooking(booking); other.setLabel(request.label().trim()); other.setAmount(request.amount().negate()); other.setIsDiscount(true); OffsetDateTime now=OffsetDateTime.now(clock); other.setCreatedAt(now);other.setUpdatedAt(now); others.saveAndFlush(other); return draft(bookingId);
 }
 private InvoiceDetailResponse build(InvoiceIssueRequest request, boolean persist) {
  if(request.items()==null||request.items().isEmpty()) throw new ValidationException("Sélectionnez au moins un élément à facturer.");
  Booking booking=(persist?bookings.findByIdForUpdate(request.bookingId()):bookings.findById(request.bookingId())).orElseThrow(()->new ResourceNotFoundException("Dossier introuvable avec l'id : "+request.bookingId()));
  BillableItemsResponse available=read.billableItems(booking.getIdBooking()); Map<String,BillableItemsResponse.Item> candidates=new HashMap<>(); available.items().forEach(x->candidates.put(key(x.sourceType(),x.sourceId()),x));available.discountItems().forEach(x->candidates.put(key(x.sourceType(),x.sourceId()),x));
  LinkedHashSet<String> selected=new LinkedHashSet<>(); for(var ref:request.items()){String key=key(ref.sourceType(),ref.sourceId());if(!selected.add(key))throw new ValidationException("Un élément ne peut être sélectionné qu'une fois.");if(!candidates.containsKey(key))throw new ConflictException("ALREADY_BILLED","Un élément sélectionné n'est plus disponible pour cette facture.");}
  List<BillableItemsResponse.Item> items=selected.stream().map(candidates::get).toList(); if(items.stream().noneMatch(x->x.amount().signum()>0)) throw new ValidationException("La facture doit contenir au moins un élément positif.");
  BigDecimal subtotal=items.stream().map(BillableItemsResponse.Item::amount).filter(x->x.signum()>0).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal discount=items.stream().map(BillableItemsResponse.Item::amount).filter(x->x.signum()<0).map(BigDecimal::abs).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal total=subtotal.subtract(discount);
  if(request.expectedTotal().compareTo(total)!=0)throw new ConflictException("TOTAL_CHANGED","Le total a changé. Vérifiez le brouillon avant de confirmer.",null,total);
  if(!persist) return previewResponse(booking,request,items,subtotal,discount,total,available);
  Invoice invoice=new Invoice(); invoice.setInvoiceType("INVOICE");invoice.setBooking(booking);invoice.setCustomer(booking.getCustomer());invoice.setStatus(statuses.findByCode("ISSUED").orElseThrow(()->new ResourceNotFoundException("Statut ISSUED introuvable.")));invoice.setIssueDate(LocalDate.now(clock));applyHeader(invoice,request,available);invoice.setSubtotal(subtotal);invoice.setDiscount(discount);invoice.setTotal(total);invoice.setIssuerSnapshot(snapshot(settings.findById(1L).orElseThrow(()->new ResourceNotFoundException("Paramètres de l'hôtel introuvables.")))); OffsetDateTime now=OffsetDateTime.now(clock);invoice.setCreatedAt(now);invoice.setUpdatedAt(now); invoice=invoices.saveAndFlush(invoice);
  int position=1; for(var item:items){InvoiceLine line=new InvoiceLine();line.setInvoice(invoice);line.setDescription(item.description());line.setQuantity(item.quantity());line.setUnitPrice(item.unitPrice());line.setPosition(position++);line.setServiceCode(item.serviceCode());line.setSourceType(item.sourceType());line.setSourceId(item.sourceId());line.setBilled(true);lines.save(line);} lines.flush(); return read.invoiceDetail(invoice.getIdInvoice());
 }
 private InvoiceDetailResponse previewResponse(Booking booking,InvoiceIssueRequest r,List<BillableItemsResponse.Item> items,BigDecimal subtotal,BigDecimal discount,BigDecimal total,BillableItemsResponse d){ List<InvoiceDetailResponse.Line> list=new ArrayList<>();int p=1;for(var x:items)list.add(new InvoiceDetailResponse.Line(null,x.description(),x.quantity(),x.unitPrice(),x.amount(),p++,x.serviceCode(),x.sourceType(),x.sourceId(),true,null));return new InvoiceDetailResponse(null,null,"INVOICE","DRAFT",LocalDate.now(clock),booking.getIdBooking(),booking.getCustomer().getIdCustomer(),r.billingName(),r.billingAddress(),r.billingPhone(),r.billingNationality(),r.billingTaxId(),r.referenceLabel(),d.periodStart(),d.periodEnd(),subtotal,discount,total,r.notes(),snapshot(settings.findById(1L).orElseThrow(()->new ResourceNotFoundException("Paramètres de l'hôtel introuvables."))),"UNPAID",BigDecimal.ZERO,total,null,List.of(),list,null); }
 private void applyHeader(Invoice i,InvoiceIssueRequest r,BillableItemsResponse d){i.setBillingName(r.billingName().trim());i.setBillingAddress(r.billingAddress());i.setBillingPhone(r.billingPhone());i.setBillingNationality(r.billingNationality());i.setBillingTaxId(r.billingTaxId());i.setReferenceLabel(r.referenceLabel()==null?d.referenceLabel():r.referenceLabel());i.setPeriodStart(d.periodStart());i.setPeriodEnd(d.periodEnd());i.setNotes(r.notes());}
 private static String key(String t,Long id){return t+":"+id;}
 static String snapshot(HotelSettings h){return "{\"legalName\":\""+esc(h.getLegalName())+"\",\"address\":\""+esc(h.getAddress())+"\",\"phone\":\""+esc(h.getPhone())+"\",\"email\":\""+esc(h.getEmail())+"\",\"nif\":\""+esc(h.getNif())+"\",\"stat\":\""+esc(h.getStat())+"\",\"paymentDetails\":\""+esc(h.getPaymentDetails())+"\",\"invoiceFooter\":\""+esc(h.getInvoiceFooter())+"\"}";}
 private static String esc(String v){return v==null?"":v.replace("\\","\\\\").replace("\"","\\\"");}
}
