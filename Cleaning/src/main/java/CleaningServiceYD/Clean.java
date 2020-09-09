package CleaningServiceYD;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Clean_table")
public class Clean {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String status;
    private Long requestId;
    private String cleanDate;

    @PostPersist
    public void onPostPersist(){
        System.out.println("##### Payment onPostPersist : " + getStatus());

        if("CleaningConfirmed".equals(getStatus())){
            CleaningConfirmed cleaningConfirmed = new CleaningConfirmed();
            BeanUtils.copyProperties(this, cleaningConfirmed);
            cleaningConfirmed.setRequestId(getRequestId());
            cleaningConfirmed.setStatus("CleaningCompleted");
            cleaningConfirmed.setCleanDate(getCleanDate());
            cleaningConfirmed.publishAfterCommit();
        }
        else if("CleaningFinished".equals(getStatus())) {
            CleaningFinished cleaningFinished = new CleaningFinished();
            BeanUtils.copyProperties(this, cleaningFinished);
            cleaningFinished.setRequestId(getRequestId());
            cleaningFinished.setStatus("CleaningFinished");
            cleaningFinished.setCleanDate(getCleanDate());

            cleaningFinished.publishAfterCommit();
        }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public String getCleanDate() {
        return cleanDate;
    }

    public void setCleanDate(String cleanDate) {
        this.cleanDate = cleanDate;
    }




}
