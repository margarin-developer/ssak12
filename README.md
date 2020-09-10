# Ssak12 - 리뷰 서비스

# Table of contents
- [ssak3]
  - [서비스 시나리오](#서비스-시나리오)
  - [분석/설계](#분석/설계)
  - [구현](#구현)
    - [DDD 의 적용](#DDD-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#CI/CD-설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
    - [ConfigMap 사용](#ConfigMap-사용)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오
  
## 기능적 요구사항
1. 청소부가 청소를 완료하면 최종 완료된다 (Sync, 리뷰서비스)
2. 청소가 최종 완료되면, 청소부가 고객에 대한 리뷰를 작성한다.
3. 고객에 대한 리뷰가 완료되면, 리뷰 내용을 청소 업체애 전달한다. (Async, 알림서비스)

## 비기능적 요구사항
### 1. 트랜잭션
- 청소가 최종 완료되지 않으면 리뷰가 성립되지 않아야 한다 → Sync 호출
### 2. 장애격리
- 알림 기능이 수행되지 않더라도 리뷰 작성은 365일 24시간 받을 수 있어야 한다 → Async (event-driven), Eventual Consistency
- 리뷰시스템이 과중되면 리뷰작성을 잠시동안 받지 않고 잠시 후에 하도록 한다  → Circuit breaker, fallback
### 3. 성능
- 리뷰의 내용을 마이페이지(프론트엔드)에서 확인할 수 있어야 한다 → CQRS 
- 상태가 바뀔때마다 알림을 줄 수 있어야 한다 → Event driven

# 분석/설계 (http://www.msaez.io/#/storming/nPLWWPCPVgYUDyEVhn42SQykdFz1/mine/94c463ba57f00086df5d2362921d3de8/-MGkjTbFB32Pt_czIZm1)
![이미지 1](https://user-images.githubusercontent.com/62707074/92624856-f12d8d80-f302-11ea-9841-e17062b71be5.png)
  
* 도메인 서열 분리 
  - Supporting Domain: 리뷰 
     - 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.  
    
* 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
    - 청소 완료 시 리뷰작성
      - 청소가 완료되지 않으면 리뷰는 작성할 수 없다는 원칙에 따라, ACID 트랜잭션 적용. 청소 완료 시 리뷰작성에 대해서는 Request-Response 방식 처리
    - 리뷰 작성 완료 시 알림 처리
      - 청소가 리뷰에서 알림 마이크로서비스로 리뷰 내용을 전달하는 과정에 알림 마이크로서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리

# 배포 (deploy)
마이크로 서비스들을 스프링부트로 구현하였다. 배포를 위한 서비스 환경은 Azure Portal과 CLI를 통해서 수행한다.

## Azure 
- Configure
```console
- Azure (http://portal.azure.com) : admin12@gkn2019hotmail.onmicrosoft.com
- AZure 포탈에서 리소스 그룹 > 쿠버네티스 서비스 생성 > 컨테이너 레지스트리 생성
- 리소스 그룹 생성 : ssak12-rg
- 컨테이너 생성( Kubernetes ) : ssak12-aks
- 레지스트리 생성 : ssak12acr, ssak12acr.azurecr.io
```
- 접속환경
  > Azure 포탈에서 가상머신 신규 생성 - ubuntu 18.04

- 모듈 설치
    - kubectl
    - Azure cli 설치 후 az 로그인 및 인증, 연결
    - jdk
    - docker
    - kafka 
    - istio : kiali도 함께 설치됨

- Azure 인증
```console
az login
az aks get-credentials --resource-group ssak12-rg --name ssak12-aks
az acr login --name ssak12acr --expose-token
```

- namespace 설정
```console
kubectl create namespace ssak12
kubectl config set-context --current --namespace=ssak12
```

- kiali - LoadBalancer로 변경
```console
kubectl edit service/kiali -n istio-system
(ClusterIP -> LoadBalancer)
- Exterlan IP 생성
service/kiali                    LoadBalancer   10.0.67.209    20.196.112.235     20001:31868/TCP  
```

- siege deploy & httpie 설치
```console
cd ssak12/yaml
kubectl apply -f siege.yaml 
kubectl exec -it siege -n ssak12 -- /bin/bash
apt-get update
apt-get install httpie
```

## image build & push
- compile
```console
cd ssak12/gateway
mvn package
```
- image build 
```console
docker build -t ssak12acr.azurecr.io/cleaning:1.0 .
docker build -t ssak12acr.azurecr.io/dashboard:1.0 .
docker build -t ssak12acr.azurecr.io/message:1.0 .
docker build -t ssak12acr.azurecr.io/payment:1.0 .
docker build -t ssak12acr.azurecr.io/reservation:1.0 .
docker build -t ssak12acr.azurecr.io/gateway:1.0 .
docker build -t ssak12acr.azurecr.io/review:1.0 .
```
- impage push (to Azure 컨테이너레지스트리)
```console
docker push ssak12acr.azurecr.io/cleaning:1.0
docker push ssak12acr.azurecr.io/dashboard:1.0
docker push ssak12acr.azurecr.io/message:1.0
docker push ssak12acr.azurecr.io/payment:1.0
docker push ssak12acr.azurecr.io/reservation:1.0
docker push ssak12acr.azurecr.io/gateway:1.0
docker push ssak12acr.azurecr.io/review:1.0
```
- impage 확인
```
- docker images 또는
- Azure 포탈에서 '컨테이너레지스트리-리포지토리'에서 생성여부 확인
```

## application deploy (using yaml)
```console
kubectl create ns ssak12

cd ssak12/yaml

kubectl apply -f configmap.yaml
kubectl apply -f cleaning.yaml
kubectl apply -f reservation.yaml
kubectl apply -f payment.yaml
kubectl apply -f dashboard.yaml
kubectl apply -f message.yaml
kubectl apply -f gateway.yaml
kubectl apply -f review.yaml
```

# 구현

## CQRS 적용
- 리뷰를 등록하고 정보를가져 오는 부분을 CQRS로 서비스된다.
```java
@Service
public class DashBoardViewViewHandler {

    @Autowired
    private DashBoardViewRepository dashBoardViewRepository;

    ...
    // Insert 추가
    @StreamListener(KafkaProcessor.INPUT)
    public void whenCleaningFinished_then_CREATE_2 (@Payload CleaningFinished cleaningFinished) {
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
    ...
    // Update 추가
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

}
```

## DDD 의 적용
* 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 신규로 개발한 리뷰 마이크로서비스).
  - 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용할 수 있지만, 일부 구현에 있어서 영문이 아닌 경우는 실행이 불가능한 경우가 있다 Maven pom.xml, Kafka의 topic id, FeignClient 의 서비스 id 등은 한글로 식별자를 사용하는 경우 오류가 발생하는 것을 확인하였다)
  - 최종적으로는 모두 영문을 사용하였으며, 이는 잠재적인 오류 발생 가능성을 차단하고 향후 확장되는 다양한 서비스들 간에 영향도를 최소화하기 위함이다.
```console
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

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package CleaningServiceYD;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReviewRepository extends PagingAndSortingRepository<Review, Long>{


}
```

- API Gateway 적용
```console
# gateway service type 변경
$ kubectl edit service/gateway -n ssak12
(ClusterIP -> LoadBalancer)

root@ssak12-vm:/home/skccadmin/ssak12# kubectl get service -n ssak12
NAME          TYPE           CLUSTER-IP     EXTERNAL-IP      PORT(S)          AGE
cleaning      ClusterIP      10.0.53.241    <none>           8080/TCP         52m
dashboard     ClusterIP      10.0.96.188    <none>           8080/TCP         51m
gateway       LoadBalancer   10.0.249.43    20.196.112.208   8080:30571/TCP   48m
message       ClusterIP      10.0.238.129   <none>           8080/TCP         50m
payment       ClusterIP      10.0.157.218   <none>           8080/TCP         50m
reservation   ClusterIP      10.0.54.226    <none>           8080/TCP         49m
review        ClusterIP      10.0.202.3     <none>           8080/TCP         49m
```
- API Gateway 적용 확인
```console
//예약
http POST http://20.196.112.208:8080/cleaningReservations requestDate=20201213 place=Bundang status=ReservationApply price=30000 customerName=Chae

// 청소완료 (추가된 부분)
http POST http://20.196.112.208:8080/cleans status=CleaningFinished requestId=3 cleanDate=20200930

// 리뷰작성 (추가된 부분)
http POST http://20.196.112.208:8080/reviews status=CleaningFinished requestId=3 content=veryGood
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Thu, 10 Sep 2020 01:15:07 GMT
Location: http://review:8080/reviews/2
transfer-encoding: chunked

{
    "_links": {
        "review": {
            "href": "http://review:8080/reviews/2"
        },
        "self": {
            "href": "http://review:8080/reviews/2"
        }
    },
    "content": "veryGood",
    "requestId": 3,
    "status": "CleaningFinished"
}
```

- siege 접속
```console
kubectl exec -it siege -n ssak4 -- /bin/bash
```
- (siege 에서) 적용 후 REST API 테스트 
```
# 청소 서비스 예약요청 처리
http POST http://reservation:8080/cleaningReservations requestDate=20200907 place=seoul status=ReservationApply price=2000 customerName=yeon

// 청소완료 (추가된 부분)
http POST http://reservation:8080/cleans status=CleaningFinished requestId=4 cleanDate=20200911

// 리뷰작성 (추가된 부분)
http POST http://reservation:8080/reviews status=CleaningFinished requestId=4 content=Excellent
```

## 폴리글랏 퍼시스턴스

  * 각 마이크로서비스의 특성에 따라 데이터 저장소를 RDB, DocumentDB/NoSQL 등 다양하게 사용할 수 있지만, 시간적/환경적 특성상 모두 H2 메모리DB를 적용하였다.

## 폴리글랏 프로그래밍
  
  * 각 마이크로서비스의 특성에 따라 다양한 프로그래밍 언어를 사용하여 구현할 수 있지만, 시간적/환경적 특성상 Java를 이용하여 구현하였다.

## 동기식 호출 과 Fallback 처리
분석단계에서의 조건 중 하나로 청소->리뷰 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 리뷰 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 - configmap 처리
```java
@FeignClient(name="Review", url="${api.url.review}")
public interface ReviewService {

    @RequestMapping(method= RequestMethod.POST, path="/reviews")
    public void reviewRequest(@RequestBody Review review);

}
```
- 리뷰 등록을 받은 직후(@PostPersist) 리뷰 등록처리가 완료되도록 처리
```java
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
```

- 호출 시간에 따른 타임 커플링이 발생하며, 리뷰 시스템이 장애가 나면 리뷰 처리도 못받는다는 것을 확인
```
# 리뷰 서비스를 잠시 내려놓음
$ kubectl delete -f review.yaml

# 리뷰작성 (추가된 부분)
http POST http://reservation:8080/reviews status=CleaningFinished requestId=4 content=Excellent

# 예약처리 시 에러 내용
HTTP/1.1 500 Internal Server Error
content-type: application/json;charset=UTF-8
date: Wed, 09 Sep 2020 13:32:42 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 87

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/cleans",
    "status": 500,
    "timestamp": "2020-09-08T15:51:34.959+0000"
}

# 리뷰서비스 재기동
$ kubectl apply -f review.yaml

NAME                           READY   STATUS    RESTARTS   AGE
review-7b59f74c46-h6q6v        2/2     Running   0          108s
siege                          2/2     Running   0          96m

# 리뷰등록 (추가된 부분)
http POST http://reservation:8080/reviews status=CleaningFinished requestId=4 content=Excellent

# 처리결과
HTTP/1.1 201 Created
content-type: application/json;charset=UTF-8
date: Wed, 09 Sep 2020 13:48:00 GMT
location: http://cleaning:8080/cleans/11
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 10

{
    "_links": {
        "clean": {
            "href": "http://cleaning:8080/cleans/11"
        },
        "self": {
            "href": "http://cleaning:8080/cleans/11"
        }
    },
    "cleanDate": null,
    "requestId": 4,
    "content": "Excellent",
    "status": "CleaningFinished"
}
```
- 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다 (서킷브레이커, 폴백 처리는 운영단계에서 설명)

## 모니터링
- kiali 접속 : http://20.196.112.235/:20001/
  ![kiali](https://user-images.githubusercontent.com/62707074/92681060-86fb0400-f367-11ea-9298-cbb5c7837328.png)


## 비동기식 호출과 Eventual Consistency
- 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
리뷰가 이루어진 후에 알림 처리는 동기식이 아니라 비 동기식으로 처리하여 알림 시스템의 처리를 위하여 예약이 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 리뷰 기록을 남긴 후에 곧바로 완료되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
```java
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

```
- 알림 서비스에서는 리뷰등록완료 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다
```java
@Service
public class PolicyHandler{
    ....
    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReviewCompleted_MessageAlert(@Payload ReviewCompleted reviewCompleted){

        if(reviewCompleted.isMe()){
            Message message = new Message();

            message.setRequestId(reviewCompleted.getRequestId());
            message.setStatus(reviewCompleted.getStatus());

            messageRepository.save(message);

            System.out.println("##### listener MessageAlert : " + reviewCompleted.toJson());
        }
    }
    ...
}
```

* 알림 시스템은 리뷰와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 알림 시스템이 유지보수로 인해 잠시 내려간 상태라도 예약을 받는데 문제가 없다

```
# 알림 서비스를 잠시 내려놓음
kubectl delete -f message.yaml

# 리뷰처리 (추가된 부분)
http POST http://reservation:8080/reviews status=CleaningFinished requestId=5 content=Bad

# 알림이력 확인
http http://message:8080/messages # 알림이력조회 불가

http: error: ConnectionError: HTTPConnectionPool(host='message', port=8080): Max retries exceeded with url: /messages (Caused by NewConnectionError('<urllib3.connection.HTTPConnection object at 0x7f7429fd2eb8>: Failed to establish a new connection: [Errno -2] Name or service not known')) while doing GET request to URL: http://message:8080/messages

# 알림 서비스 기동
kubectl apply -f message.yaml

# 알림이력 확인 (siege 에서)
http http://message:8080/messages # 알림이력조회

HTTP/1.1 200 OK
content-type: application/hal+json;charset=UTF-8
date: Wed, 09 Sep 2020 13:56:56 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 439

{
    "_embedded": {
        "messages": [
            {
                "_links": {
                    "message": {
                        "href": "http://message:8080/messages/1"
                    },
                    "self": {
                        "href": "http://message:8080/messages/1"
                    }
                },
                "requestId": 2,
                "status": "ReviewCompleted"
            },
            {
                "_links": {
                    "message": {
                        "href": "http://message:8080/messages/2"
                    },
                    "self": {
                        "href": "http://message:8080/messages/2"
                    }
                },
                "requestId": 3,
                "status": "ReviewCompleted"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://message:8080/profile/messages"
        },
        "self": {
            "href": "http://message:8080/messages{?page,size,sort}",
            "templated": true
        }
    },
    "page": {
        "number": 0,
        "size": 20,
        "totalElements": 2,
        "totalPages": 1
    }
}
```

# 운영

## CI/CD 설정
  * 각 구현체들은 github의 각각의 source repository 에 구성
  * Image repository는 Azure 사용

## 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 함
* (istio injection 적용한 경우) istio injection 적용 해제
```
kubectl label namespace ssak4 istio-injection=disabled --overwrite

# namespace/ssak4 labeled

kubectl apply -f reservation.yaml
kubectl apply -f review.yaml
```
- 리뷰 서비스 배포시 resource 설정 적용되어 있음
```
    spec:
      containers:
          ...
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```

- 리뷰 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 3개까지 늘려준다
```console
kubectl autoscale deploy review -n ssak4 --min=1 --max=3 --cpu-percent=15

horizontalpodautoscaler.autoscaling/review autoscaled
```
```console
root@ssak12-vm:~/ssak4/yaml# kubectl get all -n ssak12
NAME                                         REFERENCE           TARGETSMINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/review   Deployment/review   <unknown>/15%1         3         1          27s
```

- CB 에서 했던 방식대로 워크로드를 3분 동안 걸어준다.
```console
siege -v -c100 -t180S -r10 --content-type "application/json" 'http://review:8080/reviews POST {"requestId": "4","content": GoodGood,"status": "CleaningFinished"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```console
kubectl get deploy review -n ssak12 -w 

NAME      READY   UP-TO-DATE   AVAILABLE   AGE
payment   1/1     1            1           43m

# siege 부하 적용 후
root@ssak12-vm:/# kubectl get deploy review -n ssak12 -w
NAME      READY   UP-TO-DATE   AVAILABLE   AGE
review   1/1     1            1           43m
review   1/3     1            1           44m
review   1/3     1            1           44m
review   1/3     3            1           44m
review   2/3     3            2           46m
review   3/3     3            3           46m
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다.
```console
Lifting the server siege...
Transactions:                  19309 hits
Availability:                 100.00 %
Elapsed time:                 179.75 secs
Data transferred:               6.31 MB
Response time:                  0.92 secs
Transaction rate:             107.42 trans/sec
Throughput:                     0.04 MB/sec
Concurrency:                   99.29
Successful transactions:       19309
Failed transactions:               0
Longest transaction:            7.33
Shortest transaction:           0.01
```

## 무정지 재배포 (readness)
- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함 (위의 시나리오에서 제거되었음)
```console
kubectl delete horizontalpodautoscaler.autoscaling/review -n ssak12
```
- yaml 설정 참고
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: review
  namespace: ssak12
  labels:
    app: review
spec:
  replicas: 1
  selector:
    matchLabels:
      app: review
  template:
...
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```console
siege -v -c1 -t120S -r10 --content-type "application/json" 'http://review:8080/reviews POST {"requestId": "3","content": GoodGood,"status": "CleaningFinished"}'
```

- 새버전으로의 배포 시작
```
# 컨테이너 이미지 Update (readness, liveness 미설정 상태)
kubectl apply -f reviews_na.yaml
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```console
Lifting the server siege...
Transactions:                  22984 hits
Availability:                  98.68 %
Elapsed time:                 299.64 secs
Data transferred:               7.52 MB
Response time:                  0.01 secs
Transaction rate:              76.71 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                    0.97
Successful transactions:       22984
Failed transactions:             308
Longest transaction:            0.97
Shortest transaction:           0.00

```

- 배포기간중 Availability 가 평소 100%에서 98% 대로 떨어지는 것을 확인. 

- 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:
```console
kubectl apply -f review.yaml

NAME                           READY   STATUS    RESTARTS   AGE
review-84bcbdfd47-bsw9q      1/1     Running   0          46s
```
- 동일한 시나리오로 재배포 한 후 Availability 확인
```console
Lifting the server siege...
Transactions:                   6663 hits
Availability:                 100.00 %
Elapsed time:                 119.51 secs
Data transferred:               2.17 MB
Response time:                  0.02 secs
Transaction rate:              55.75 trans/sec
Throughput:                     0.02 MB/sec
Concurrency:                    0.98
Successful transactions:        6663
Failed transactions:               0
Longest transaction:            0.86
Shortest transaction:           0.00
```

- 배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.

## ConfigMap 사용
- 시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리합니다.
- configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ssak4-config
  namespace: ssak4
data:
  api.url.payment: http://payment:8080
```

```
root@ssak4-vm:/home/skccadmin/ssak4/yaml# cat configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ssak4-config
  namespace: ssak4
data:
  api.url.review: http://review:8080
  api.url.payment: http://payment:8080root@ssak4-vm:/home/skccadmin/ssak4/yaml# kubectl describe configmap ssak4-config
Name:         ssak4-config
Namespace:    ssak4
Labels:       <none>
Annotations:  <none>

Data
====
api.url.payment:
----
http://payment:8080
api.url.review:
----
http://review:8080
```
- cleaning.yaml (configmap 사용)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cleaning
  namespace: ssak4
  labels:
    app: cleaning
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cleaning
  template:
    metadata:
      labels:
        app: cleaning
    spec:
      containers:
        - name: cleaning
          image: ssak4acr.azurecr.io/cleaning:1.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: api.url.review
              valueFrom:
                configMapKeyRef:
                  name: ssak4-config
                  key: api.url.review
```

- configmap 설정 정보 확인
```console
kubectl describe pod/cleaning-bfc87d78f-7frt2 -n ssak4

...중략
Containers:
  cleaning:
    Container ID:   docker://ed6c502a3421b92d64bc13da2e3856ca08233d60d82ab55c95c93f61d9ea848a
    Image:          ssak4acr.azurecr.io/cleaning:1.0
    Image ID:       docker-pullable://ssak4acr.azurecr.io/cleaning@sha256:dfe6ab86f913faf7ab35b2881121e444ff332be9263385efa74ae17e77d5f24d
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Wed, 09 Sep 2020 14:40:45 +0000
    Ready:          True
    Restart Count:  0
    Liveness:       http-get http://:8080/actuator/health delay=120s timeout=2s period=5s #success=1 #failure=5
    Readiness:      http-get http://:8080/actuator/health delay=10s timeout=2s period=5s #success=1 #failure=10
    Environment:
      api.url.review:  <set to the key 'api.url.review' of config map 'ssak4-config'>  Optional: false
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-qd9hz (ro)
...중략
```

## 동기식 호출 / 서킷 브레이킹 / 장애격리

### 서킷 브레이킹 프레임워크의 선택: istio-injection + DestinationRule

* istio-injection 적용 (기 적용완료)
```
kubectl label namespace ssak4 istio-injection=enabled

# error: 'istio-injection' already has a value (enabled), and --overwrite is false
```
* 리뷰 서비스 모두 아무런 변경 없음

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시
```console
siege -v -c100 -t60S -r10 --content-type "application/json" 'http://cleaning:8080/cleans POST {"requestId": "3","reviewDate": 20200910,"status": "CleaningReview"}'

HTTP/1.1 201     0.23 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.11 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.12 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.17 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.10 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.11 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.14 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.09 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.10 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.14 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.12 secs:     269 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 201     0.33 secs:     269 bytes ==> POST http://cleaning:8080/cleans

Lifting the server siege...
Transactions:                  27491 hits
Availability:                 100.00 %
Elapsed time:                  59.12 secs
Data transferred:               7.03 MB
Response time:                  0.20 secs
Transaction rate:             465.00 trans/sec
Throughput:                     0.12 MB/sec
Concurrency:                   92.90
Successful transactions:       27502
Failed transactions:               0
Longest transaction:            2.27
Shortest transaction:           0.00
```
* 서킷 브레이킹을 위한 DestinationRule 적용
```
cd ssak4/yaml
kubectl apply -f review_dr.yaml

destinationrule.networking.istio.io/dr-review created
```
```
HTTP/1.1 500     0.68 secs:     262 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 500     0.70 secs:     262 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 500     0.71 secs:     262 bytes ==> POST http://cleaning:8080/cleans
HTTP/1.1 500     0.72 secs:     262 bytes ==> POST http://cleaning:8080/cleans

siege aborted due to excessive socket failure; you
can change the failure threshold in $HOME/.siegerc

Transactions:                     20 hits
Availability:                   1.75 %
Elapsed time:                   9.92 secs
Data transferred:               0.29 MB
Response time:                 48.04 secs
Transaction rate:               2.02 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   96.85
Successful transactions:          20
Failed transactions:            1123
Longest transaction:            2.53
Shortest transaction:           0.04
```

- DestinationRule 적용 실패ㅠㅠ

# 개인 MSA
1. 고객관리 (이름, 주소 등등) => 연제경 
2. 청소부 관리 (이름, 핸드폰 번호 등등) => 노필호
3. 청소부 리뷰관리 (리뷰, 별점평가 등등) => 성은주
4. 고객 리뷰관리 (리뷰, 별점평가 등등) => 채민호
5. 결제 관리 (카드, 무통장 등등) => 박유리
