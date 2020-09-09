package CleaningServiceYD;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long requestId;
    private Integer price;
    private String status;

    @PostPersist
    public void onPostPersist(){

    	System.out.println("##### Payment onPostPersist : " + getStatus());

    	if("PaymentApproved".equals(getStatus())) {

        	PayConfirmed payConfirmed = new PayConfirmed();
            BeanUtils.copyProperties(this, payConfirmed);
            payConfirmed.setRequestId(getRequestId());
            payConfirmed.setStatus("PaymentCompleted");
            payConfirmed.publishAfterCommit();
        }

        else if("PaymentCancel".equals(getStatus())) {
        	PayCancelConfirmed payCancelConfirmed = new PayCancelConfirmed();
            BeanUtils.copyProperties(this, payCancelConfirmed);
            payCancelConfirmed.setRequestId(getRequestId());
            payCancelConfirmed.setStatus("PaymentCancelCompleted");
            payCancelConfirmed.publishAfterCommit();
        }

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
