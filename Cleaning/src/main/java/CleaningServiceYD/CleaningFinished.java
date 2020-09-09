package CleaningServiceYD;

public class CleaningFinished extends AbstractEvent {

    private Long id;
    private String status;
    private Long requestID;
    private String cleanDate;

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
        return requestID;
    }
    public void setRequestId(Long requestID) {
        this.requestID = requestID;
    }

    public String getCleanDate() {
        return cleanDate;
    }
    public void setCleanDate(String cleanDate) {
        this.cleanDate = cleanDate;
    }
}