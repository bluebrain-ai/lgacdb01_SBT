package com.bluescript.demo;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.swagger.annotations.ApiResponses;

import com.bluescript.demo.jpa.IinsertCustomerJpa;
import com.bluescript.demo.model.WsHeader;
import com.bluescript.demo.model.ErrorMsg;
import com.bluescript.demo.model.EmVariable;
import com.bluescript.demo.model.Cdb2Area;
import com.bluescript.demo.model.Db2Customer;
import com.bluescript.demo.model.Dfhcommarea;

@Getter
@Setter
@RequiredArgsConstructor
@Log4j2
@Component

@RestController
@RequestMapping("/")
@ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 400, message = "This is a bad request, please follow the API documentation for the proper request format"),
        @io.swagger.annotations.ApiResponse(code = 401, message = "Due to security constraints, your access request cannot be authorized"),
        @io.swagger.annotations.ApiResponse(code = 500, message = "The server/Application is down. Please contact support team.") })
        @CrossOrigin(origins = "*", allowedHeaders = "*")
public class Lgacdb01 {

    @Autowired
    private WsHeader wsHeader;
    @Autowired
    private ErrorMsg errorMsg;
    @Autowired
    private EmVariable emVariable;
    @Autowired
    private Cdb2Area cdb2Area;
    @Autowired
    private Db2Customer db2Customer;
    @Autowired
    private Dfhcommarea dfhcommarea;

    private int wsResp;
    private long lastcustnum;
    private static final String genacount = "GENACUSTNUM";
    private String wsTime;
    private String wsDate;
    private String caData;
    private String lgacNcs = "ON";
    private int wsCaHeaderLen = 0;
    private int wsRequiredCaLen = 0;
    private int wsCustomerLen = 0;
    private int db2CustomernumInt;
    private int eibcalen;
    private String caErrorMsg;
    @Autowired
    private IinsertCustomerJpa insertCustomerJpa;
    @Value("${api.LGSTSQ.host}")
    private String LGSTSQ_HOST;
    @Value("${api.LGSTSQ.uri}")
    private String LGSTSQ_URI;
    @Value("${api.GENACOUNT.uri}")
    private String GENACOUNT_URI;
    @Value("${api.GENACOUNT.host}")
    private String GENACOUNT_HOST;

    @Value("${api.LGACVS01.host}")
    private String LGACVS01_HOST;
    @Value("${api.LGACVS01.uri}")
    private URI LGACVS01_URI;
    @Value("${api.LGACDB02.host}")
    private String LGACDB02_HOST;
    @Value("${api.LGACDB02.uri}")
    private URI LGACDB02_URI;
   private String wsAbstime;

    @PostMapping("/lgacdb01")
    public void main(@RequestBody Dfhcommarea payload) {
        // if( eibcalen == 0 )
        // {
        // errorMsg.setEmVariable(" NO COMMAREA RECEIVED"); writeErrorMessage();
        // log.error(Error code : LGCA)
        // throw new LGCAException("LGCA");

        // }
        BeanUtils.copyProperties(payload, dfhcommarea);
        wsRequiredCaLen = wsCaHeaderLen + wsRequiredCaLen;
        wsRequiredCaLen = wsCustomerLen + wsRequiredCaLen;

        obtainCustomerNumber();
        insertCustomer();
        WebClient webclientBuilder = WebClient.create(LGACVS01_HOST);
        try {
            Mono<Dfhcommarea> lgacvs01Resp = webclientBuilder.post().uri(LGACVS01_URI)
                    .body(Mono.just(dfhcommarea), Dfhcommarea.class).retrieve().bodyToMono(Dfhcommarea.class)
                    .timeout(Duration.ofMillis(10_000));
            dfhcommarea = lgacvs01Resp.block();
        } catch (Exception e) {
            log.error(e);
        }
        cdb2Area.setD2CustomerNum(db2CustomernumInt);
        cdb2Area.setD2RequestId("02ACUS");
        cdb2Area.setD2CustsecrPass("5732FEC825535EEAFB8FAC50FEE3A8AA");
        cdb2Area.setD2CustsecrCount("0000");
        cdb2Area.setD2CustsecrState("N");
        WebClient webclientBuilder_lga = WebClient.create(LGACDB02_HOST);
        try {
            Mono<Cdb2Area> lgacdb02Resp = webclientBuilder_lga.post().uri(LGACDB02_URI)
                    .body(Mono.just(cdb2Area), Cdb2Area.class).retrieve().bodyToMono(Cdb2Area.class)
                    .timeout(Duration.ofMillis(10_000));
            cdb2Area = lgacdb02Resp.block();
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException("LGACDB02_HOST connection issue");
        }

    }

    public void obtainCustomerNumber() {
        log.debug("MethodobtainCustomerNumberstarted..");
        try {
            WebClient webClientBuilder = WebClient.create(GENACOUNT_HOST);
            Mono<Long> genacountResp = webClientBuilder.post().uri(GENACOUNT_URI)
                    .body(Mono.just(lastcustnum), Long.class).retrieve().bodyToMono(Long.class)
                    .timeout(Duration.ofMillis(10_000));
            lastcustnum = genacountResp.block();
            log.debug("GENACOUNTResp:", genacountResp);
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException("GENACOUNT_HOST: issue");
        }

        if (wsResp == 0) {
            lgacNcs = "NO";
        } else {
            db2CustomernumInt = (int) lastcustnum;
        }

        log.debug("Method obtainCustomerNumber completed..");
    }

    @Transactional(readOnly = true)
    public void insertCustomer() {
        log.debug("MethodinsertCustomerstarted..");
        emVariable.setEmSqlreq(" INSERT CUSTOMER");
        if (lgacNcs == "ON") {

        }

        try {
            insertCustomerJpa.insertCustomerForDb2CustomernumIntAndCaFirstNameAndCaLastName(db2CustomernumInt,
                    dfhcommarea.getCaCustomerRequest().getCaFirstName(),
                    dfhcommarea.getCaCustomerRequest().getCaLastName(), 
                    dfhcommarea.getCaCustomerRequest().getCaDob(),
                    dfhcommarea.getCaCustomerRequest().getCaHouseName(),
                    dfhcommarea.getCaCustomerRequest().getCaHouseNum(),
                    dfhcommarea.getCaCustomerRequest().getCaPostcode(),
                    dfhcommarea.getCaCustomerRequest().getCaPhoneHome(),
                    dfhcommarea.getCaCustomerRequest().getCaPhoneMobile(),
                    dfhcommarea.getCaCustomerRequest().getCaEmailAddress());
        } catch (Exception e) {
            log.error(e);
            writeErrorMessage();
        }

        // //EXEC SQL
        // SET :DB2-CUSTOMERNUM-INT = IDENTITY_VAL_LOCAL()
        // END-EXEC

        dfhcommarea.setCaCustomerNum(db2CustomernumInt);

        log.debug("Method insertCustomer completed..");
    }

    public void writeErrorMessage() {
        log.debug("MethodwriteErrorMessagestarted..");
        wsAbstime = LocalTime.now().toString();
        wsAbstime = LocalTime.now().toString();
        wsDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMDDYYYY"));
        wsTime = LocalTime.now().toString();
        errorMsg.setEmDate(wsDate.substring(0, 8));
        errorMsg.setEmTime(wsTime.substring(0, 6));
        WebClient webclientBuilder = WebClient.create(LGSTSQ_HOST);
        try {
            Mono<ErrorMsg> lgstsqResp = webclientBuilder.post().uri(LGSTSQ_URI)
                    .body(Mono.just(errorMsg), ErrorMsg.class).retrieve().bodyToMono(ErrorMsg.class)
                    .timeout(Duration.ofMillis(10_000));
            errorMsg = lgstsqResp.block();
        } catch (Exception e) {
            log.error(e);
        }
        if (eibcalen > 0) {
            if (eibcalen < 91) {
                try {
                    Mono<ErrorMsg> lgstsqResp = webclientBuilder.post().uri(LGSTSQ_URI)
                            .body(Mono.just(errorMsg), ErrorMsg.class).retrieve().bodyToMono(ErrorMsg.class)
                            .timeout(Duration.ofMillis(10_000));
                    errorMsg = lgstsqResp.block();
                } catch (Exception e) {
                    log.error(e);
                }

            } else {
                try {
                    Mono<String> lgstsqResp = webclientBuilder.post().uri(LGSTSQ_URI)
                            .body(Mono.just(caErrorMsg), String.class).retrieve().bodyToMono(String.class)
                            .timeout(Duration.ofMillis(10_000));
                    caErrorMsg = lgstsqResp.block();
                } catch (Exception e) {
                    log.error(e);
                }

            }

        }

        log.debug("Method writeErrorMessage completed..");
    }

    /* End of program */
}