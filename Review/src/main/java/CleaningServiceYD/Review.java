package CleaningServiceYD;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Review_table")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long requestId;
    private String content;
    private String status;

    @PostPersist
    public void onPostPersist() {

        System.out.println("##### Review onPostPersist : " + getStatus());

        RevieweCompleted revieweCompleted = new RevieweCompleted();
        BeanUtils.copyProperties(this, revieweCompleted);
        revieweCompleted.setRequestId(getRequestId());
        revieweCompleted.setContent(getContent());
        revieweCompleted.setStatus("ReviewCompleted");
        revieweCompleted.publishAfterCommit();
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

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {this.status = status;}
}
