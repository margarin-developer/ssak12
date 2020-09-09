package CleaningServiceYD;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="CleaningReservation_table")
public class CleaningReservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String requestDate;
    private String place;
    private String status;
    private Integer price;
    private String customerName;

    @PostPersist
    public void onPostPersist(){
    	CleaningServiceYD.external.Payment payment = new CleaningServiceYD.external.Payment();
        payment.setRequestId(getId());
        payment.setPrice(getPrice());
        payment.setStatus("PaymentApproved");

        try {
        	ReservationApplication.applicationContext.getBean(CleaningServiceYD.external.PaymentService.class)
            	.payRequest(payment);
        } catch(Exception e) {
        	throw new RuntimeException("PaymentApprove failed. Check your payment Service.");
        }

    }

    @PostRemove
    public void onPostRemove(){
        CleaningRequestCanceled cleaningRequestCanceled = new CleaningRequestCanceled();
        BeanUtils.copyProperties(this, cleaningRequestCanceled);
        cleaningRequestCanceled.setRequestId(getId());
        cleaningRequestCanceled.setStatus("ReservationCancel");
        cleaningRequestCanceled.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }
    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }




}
