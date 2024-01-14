package ru.netology.cardtocardservice;

import lombok.ToString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.cardtocardservice.domain.OperationInfo;
import ru.netology.cardtocardservice.domain.TransferAmount;
import ru.netology.cardtocardservice.domain.TransferInfo;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CardToCardServiceApplicationTests {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Container
    private static final GenericContainer<?> cardService = new GenericContainer<>("cardservice:1.0")
            .withExposedPorts(5500);

    @BeforeEach
    void setUp() {
        cardService.start();
    }

    @AfterAll
    static void afterAll() {
        cardService.stop();
    }


    @Test
    void contextLoads_transfer_tansactionCreateTest() {

        Integer hostPort = cardService.getMappedPort(5500);
        System.out.println("Host port: " + hostPort);

        TransferInfo transferInfo = getTransferObj();
        String urlRestService = String.format("http://localhost:%s/transfer", hostPort);

        //create transaction - good scenario
        ResponseEntity<OperationInfo> createResponse = testRestTemplate.postForEntity(urlRestService, transferInfo, OperationInfo.class);

        Assertions.assertNotNull(createResponse);
        Assertions.assertEquals(createResponse.getStatusCode(), HttpStatus.OK);
        Assertions.assertNotNull(createResponse.getBody().getOperationId());

        //create transaction - bad scenarion 1. Account debet is absent
        transferInfo.setCardFromNumber("4548987854651323");
        ResponseEntity<OperationInfo> createResponseException1 = testRestTemplate.postForEntity(urlRestService, transferInfo, OperationInfo.class);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, createResponseException1.getStatusCode());

        //create transaction - bad scenarion 1. Account credit is absent
        transferInfo.setCardToNumber("4548987854653371");
        ResponseEntity<OperationInfo> createResponseException2 = testRestTemplate.postForEntity(urlRestService, transferInfo, OperationInfo.class);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, createResponseException2.getStatusCode());

        transferInfo = getTransferObj();
        transferInfo.setCardFromValidTill("0123");
        ResponseEntity<OperationInfo> createResponseException3 = testRestTemplate.postForEntity(urlRestService, transferInfo, OperationInfo.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, createResponseException3.getStatusCode());


        transferInfo.setCardFromValidTill("01/23");
        ResponseEntity<OperationInfo> createResponseException4 = testRestTemplate.postForEntity(urlRestService, transferInfo, OperationInfo.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, createResponseException4.getStatusCode());
    }

    @Test
    void contextLoads_confirm_tansactionConfirmTest() {
        Integer hostPort = cardService.getMappedPort(5500);
        System.out.println("Host port: " + hostPort);

        TransferInfo transferInfo = getTransferObj();
        String urlRestCreateTranService = String.format("http://localhost:%s/transfer", hostPort);

        //create transaction - good scenario
        ResponseEntity<OperationInfo> createResponse = testRestTemplate.postForEntity(urlRestCreateTranService, transferInfo, OperationInfo.class);
        OperationInfo operationInfo = createResponse.getBody();
        operationInfo.setCode("0000");

        if (createResponse.getStatusCode().equals(HttpStatus.OK)) {
            //test the commit
            String urlRestCommitService = String.format("http://localhost:%s/confirmOperation", hostPort);
            ResponseEntity<OperationInfo> commitResponse = testRestTemplate.postForEntity(urlRestCommitService,
                    operationInfo,
                    OperationInfo.class);
            Assertions.assertEquals(HttpStatus.OK, commitResponse.getStatusCode());
            Assertions.assertNotNull(commitResponse.getBody().getOperationId());

            //test the rollback
            //create transaction - good scenario
            ResponseEntity<OperationInfo> createResponse2 = testRestTemplate.postForEntity(urlRestCreateTranService, transferInfo, OperationInfo.class);
            OperationInfo operationInfo2 = createResponse2.getBody();

            operationInfo2.setCode("0001");
            ResponseEntity<OperationInfo> commitResponse2 = testRestTemplate.postForEntity(urlRestCommitService,
                    operationInfo2,
                    OperationInfo.class);

            Assertions.assertEquals(HttpStatus.OK, commitResponse2.getStatusCode());
            Assertions.assertNotNull(commitResponse2.getBody().getOperationId());

            //test the exception unknown action for transaction
            ResponseEntity<OperationInfo> createResponse3 = testRestTemplate.postForEntity(urlRestCreateTranService, transferInfo, OperationInfo.class);
            OperationInfo operationInfo3 = createResponse3.getBody();

            operationInfo3.setCode("0005");
            ResponseEntity<OperationInfo> commitResponse3 = testRestTemplate.postForEntity(urlRestCommitService,
                    operationInfo3,
                    OperationInfo.class);
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, commitResponse3.getStatusCode());

        }
    }

    private TransferInfo getTransferObj() {
        TransferAmount transferAmount = new TransferAmount();
        transferAmount.setValue(100);
        transferAmount.setCurrency("RUR");

        TransferInfo transferInfo = new TransferInfo();
        transferInfo.setCardFromNumber("4548987854653322");
        transferInfo.setCardToNumber("4548987854653311");
        transferInfo.setCardFromCVV("956");
        transferInfo.setCardFromValidTill("08/25");
        transferInfo.setAmount(transferAmount);
        return transferInfo;
    }

}
