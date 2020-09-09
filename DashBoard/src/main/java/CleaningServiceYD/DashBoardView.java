package CleaningServiceYD;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="DashBoardView_table")
public class DashBoardView {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long requestId;
    private Integer price;
    private String customerName;
    private String status;
    private String content;


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
    void setStatus(String status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
}
