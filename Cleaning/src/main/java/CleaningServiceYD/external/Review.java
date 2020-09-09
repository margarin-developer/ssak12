package CleaningServiceYD.external;

public class Review {

    private Long id;
    private Long requestId;
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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getContent() { return content; }
    public void setContent(String status) {
        this.status = content;
    }
}
