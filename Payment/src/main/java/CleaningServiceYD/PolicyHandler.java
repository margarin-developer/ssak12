package CleaningServiceYD;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import CleaningServiceYD.config.kafka.KafkaProcessor;

@Service
public class PolicyHandler{

	@Autowired
    private PaymentRepository paymentRepository;

	/*
	 * @StreamListener(KafkaProcessor.INPUT) public void
	 * onStringEventListener(@Payload String eventString){
	 *
	 * }
	 */

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCleaningRequestCanceled_PayCancelRequest(@Payload CleaningRequestCanceled cleaningRequestCanceled){

    	if(cleaningRequestCanceled.isMe()){
            Payment payment = new Payment();
            payment.setRequestId(cleaningRequestCanceled.getRequestId());
            payment.setStatus("PaymentCancel");

            paymentRepository.save(payment);
        }
    }

}
