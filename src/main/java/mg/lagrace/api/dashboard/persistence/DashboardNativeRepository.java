package mg.lagrace.api.dashboard.persistence;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos;
import mg.lagrace.api.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import mg.lagrace.api.dashboard.dto.dashboard.DashboardDtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native, read-only queries used only by the operations dashboard. */
@Repository
public class DashboardNativeRepository {
  private final JdbcTemplate jdbc;
  public DashboardNativeRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
  public Progress arrivals(LocalDate d){return jdbc.queryForObject("select count(*),count(*) filter(where s.code in ('CHECK_IN','CHECK_OUT')) from booking_room r join booking_line_status s on s.id_booking_line_status=r.status_id where r.is_active and r.check_in=?",(rs,n)->new Progress(rs.getLong(1),rs.getLong(2)),d);}
  public Progress departures(LocalDate d){return jdbc.queryForObject("select count(*),count(*) filter(where s.code='CHECK_OUT') from booking_room r join booking_line_status s on s.id_booking_line_status=r.status_id where r.is_active and r.check_out=?",(rs,n)->new Progress(rs.getLong(1),rs.getLong(2)),d);}
  public Occupancy occupied(LocalDate d){return jdbc.queryForObject("select count(*) filter(where s.code='CHECK_IN'), (select count(*) from room where not is_deleted) from booking_room r join booking_line_status s on s.id_booking_line_status=r.status_id where r.is_active and r.check_in<=? and r.check_out>?",(rs,n)->{long o=rs.getLong(1),t=rs.getLong(2);return new Occupancy(o,t,t==0?BigDecimal.ZERO:BigDecimal.valueOf(o*100.0/t));},d,d);}
  public BigDecimal amountToCollect(){return jdbc.queryForObject("select coalesce(sum(greatest(balance,0)),0) from v_booking_balance x join booking b on b.id_booking=x.id_booking join booking_status s on s.id_booking_status=b.status_id where s.code in ('PENDING','CONFIRMED','IN_PROGRESS')",BigDecimal.class);}
  public BigDecimal collected(LocalDate d){return jdbc.queryForObject("select coalesce(sum(case when k.code in ('PAYMENT','ADVANCE') then p.amount when k.code='REFUND' then -p.amount else 0 end),0) from payment p join payment_kind k on k.id_kind=p.kind_id where (p.paid_at at time zone 'Indian/Antananarivo')::date=?",BigDecimal.class,d);}
  public long taskCount(LocalDate d){return jdbc.queryForObject("select (select count(*) from booking b join booking_status s on s.id_booking_status=b.status_id where s.code='PENDING')+(select count(*) from booking_room r join booking_line_status s on s.id_booking_line_status=r.status_id where r.is_active and s.code='CONFIRMED' and r.check_in<?)+(select count(*) from booking_room r join booking_line_status s on s.id_booking_line_status=r.status_id where r.is_active and s.code='CHECK_IN' and r.check_out<?)",Long.class,d,d);}
  public List<ArrivalDetail> arrivalsList(LocalDate d){return jdbc.query("select b.id_booking,concat_ws(' ',c.firstname,c.lastname),r.ref_room,rt.name,br.nights, exists(select 1 from payment p join payment_kind k on k.id_kind=p.kind_id where p.booking_id=b.id_booking and k.code='ADVANCE'),x.balance,s.name from booking_room br join booking b on b.id_booking=br.booking_id join customer c on c.id_customer=b.customer_id join room r on r.id_room=br.room_id join room_type rt on rt.id_type=r.type_id join booking_line_status s on s.id_booking_line_status=br.status_id join v_booking_balance x on x.id_booking=b.id_booking where br.is_active and br.check_in=? order by r.ref_room limit 50",(rs,n)->new ArrivalDetail(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getBoolean(6),rs.getBigDecimal(7),rs.getString(8)),d);}
  public List<DepartureDetail> departuresList(LocalDate d){return jdbc.query("select b.id_booking,concat_ws(' ',c.firstname,c.lastname),r.ref_room,x.balance, exists(select 1 from v_booking_line l where l.booking_id=b.id_booking and l.is_active),s.name from booking_room br join booking b on b.id_booking=br.booking_id join customer c on c.id_customer=b.customer_id join room r on r.id_room=br.room_id join booking_line_status s on s.id_booking_line_status=br.status_id join v_booking_balance x on x.id_booking=b.id_booking where br.is_active and br.check_out=? order by r.ref_room limit 50",(rs,n)->new DepartureDetail(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),rs.getBoolean(5),rs.getString(6)),d);}
  public List<RoomLive> roomsLive(LocalDate d){String sql="select x.id_booking,r.ref_room,rt.name,coalesce(x.state,'FREE'),x.client,x.check_in,x.check_out,x.balance from room r join room_type rt on rt.id_type=r.type_id left join lateral(select b.id_booking,concat_ws(' ',c.firstname,c.lastname) client,br.check_in,br.check_out,bal.balance,case when s.code='CHECK_IN' and br.check_out=? then 'LEAVING' when s.code='CHECK_IN' then 'OCCUPIED' when s.code='CONFIRMED' and br.check_in=? then 'ARRIVING' when s.code='CONFIRMED' then 'RESERVED' end state from booking_room br join booking b on b.id_booking=br.booking_id join customer c on c.id_customer=b.customer_id join booking_line_status s on s.id_booking_line_status=br.status_id join v_booking_balance bal on bal.id_booking=b.id_booking where br.room_id=r.id_room and br.is_active and ((s.code='CHECK_IN' and br.check_in<=? and br.check_out>?) or(s.code='CONFIRMED' and br.check_in>=?)) order by br.check_in limit 1)x on true where not r.is_deleted order by r.ref_room";return jdbc.query(sql,(rs,n)->new RoomLive((Long)rs.getObject(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getObject(6,LocalDate.class),rs.getObject(7,LocalDate.class),rs.getBigDecimal(8)),d,d,d,d,d);}
}
