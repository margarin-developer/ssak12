package CleaningServiceYD;

import CleaningServiceYD.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DashBoardViewViewHandler {


    @Autowired
    private DashBoardViewRepository dashBoardViewRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayConfirmed_then_CREATE_1 (@Payload PayConfirmed payConfirmed) {
        try {
            if (payConfirmed.isMe()) {
                // view 객체 생성
                DashBoardView dashBoardView = new DashBoardView();
                // view 객체에 이벤트의 Value 를 set 함
                dashBoardView.setRequestId(payConfirmed.getRequestId());
                dashBoardView.setStatus(payConfirmed.getStatus());
                dashBoardView.setPrice(payConfirmed.getPrice());
                // view 레파지 토리에 save
                dashBoardViewRepository.save(dashBoardView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningFinished_then_CREATE_1 (@Payload CleaningFinished cleaningFinished) {
        try {
            if (cleaningFinished.isMe()) {
                // view 객체 생성
                DashBoardView dashBoardView = new DashBoardView();
                // view 객체에 이벤트의 Value 를 set 함
                dashBoardView.setRequestId(cleaningFinished.getRequestId());
                dashBoardView.setStatus(cleaningFinished.getStatus());
                // view 레파지 토리에 save
                dashBoardViewRepository.save(dashBoardView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayCancelConfirmed_then_UPDATE_1(@Payload PayCancelConfirmed payCancelConfirmed) {
        try {
            if (payCancelConfirmed.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(payCancelConfirmed.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(payCancelConfirmed.getStatus());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningConfirmed_then_UPDATE_2(@Payload CleaningConfirmed cleaningConfirmed) {
        try {
            if (cleaningConfirmed.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(cleaningConfirmed.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(cleaningConfirmed.getStatus());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReviewCompleted_then_UPDATE_3(@Payload RevieweCompleted revieweCompleted) {
        try {
            if (revieweCompleted.isMe()) {
                // view 객체 조회
                List<DashBoardView> dashBoardViewList = dashBoardViewRepository.findByRequestId(revieweCompleted.getRequestId());
                for(DashBoardView dashBoardView : dashBoardViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashBoardView.setStatus(revieweCompleted.getStatus());
                    dashBoardView.setContent(revieweCompleted.getContent());
                    // view 레파지 토리에 save
                    dashBoardViewRepository.save(dashBoardView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningRequestCanceled_then_DELETE_1(@Payload CleaningRequestCanceled cleaningRequestCanceled) {
        try {
            if (cleaningRequestCanceled.isMe()) {
                // view 레파지 토리에 삭제 쿼리
                dashBoardViewRepository.deleteByRequestId(cleaningRequestCanceled.getRequestId());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}