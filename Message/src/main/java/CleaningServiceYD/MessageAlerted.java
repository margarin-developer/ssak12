package CleaningServiceYD;

public class MessageAlerted extends AbstractEvent {

    private Long id;
    private Long requestId;
    private String status;

    public MessageAlerted(){
        super();
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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
