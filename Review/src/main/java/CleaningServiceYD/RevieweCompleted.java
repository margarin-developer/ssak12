
package CleaningServiceYD;

public class RevieweCompleted extends AbstractEvent {

    private Long id;
    private Long requestID;
    private String content;
    private String status;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestID;
    }
    public void setRequestId(Long requestID) {
        this.requestID = requestID;
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
