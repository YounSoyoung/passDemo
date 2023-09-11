package com.example.demo.nice.controller;

import NiceID.Check.CPClient;
import com.example.demo.dto.PhoneAuthResultDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Properties;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {
    private final NiceProperties niceProperties;

    private final Properties properties;

    private final Environment environment;

    @Value("${nice.site-code}")
    private String siteCode;

    @GetMapping("/test")
    void getTest() {
        environment.getProperty("nice.site-code", "not found");
        log.info(niceProperties.getSiteCode());
        log.info(environment.getProperty("nice.site-code", "not found"));
        log.info(siteCode);
    }

    @GetMapping(value = "/pass/test")
    public String getPassTestPage(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        CPClient niceCheck = new CPClient();

        String sSiteCode = niceProperties.getSiteCode();      //Nice로부터 부여받은 사이트 코드
        String sSitePassword = niceProperties.getPassword();      //Nice로부터 부여받은 사이트 패스워드

        String sRequestNumber = niceCheck.getRequestNO(sSiteCode);      //요청 번호, 이는 성공/실패 후에 같은 값으로 되돌려주게 되므로

        String sAuthType = "";      //없으면 기본 선택화면, M: 핸드폰, C: 신용카드, X: 공인인증서

        String popgubun = "N";      //Y: 취소버튼 있음 / N: 취소버튼 없음
        String customize = "";      //없으면 기본 웹페이지 / Mobile: 모바일 페이지
        String sGender = "";        //업으면 기본 선택 값, 0: 여자, 1: 남자

        String sReturnUrl = "http://localhost:8083/pass/success";       //성공 시 이동될 URL
        String sErrorUrl = "http://localhost:8083/pass/fail";       //실패 시 이동될 URL

        //입력된 plain 데이터를 만든다. -> 요청 규격에 맞게 정리
        String sPlainData = "7:REQ_SEQ" + sRequestNumber.getBytes().length + ":" + sRequestNumber +
                            "8:SITECODE" + sSiteCode.getBytes().length + ":" + sSiteCode +
                            "9:AUTH_TYPE" + sAuthType.getBytes().length + ":" + sAuthType +
                            "7:RTN_URL" + sReturnUrl.getBytes().length + ":" + sReturnUrl +
                            "7:ERR_URL" + sErrorUrl.getBytes().length + ":" + sErrorUrl +
                            "11:POPUP_GUBUN" + popgubun.getBytes().length + ":" + popgubun +
                            "9:CUSTOMIZE" + customize.getBytes().length + ":" + customize +
                            "6:GENDER" + sGender.getBytes().length + ":" + sGender;

        String sMessage = "";
        String sEncData = "";       //회원사 정보를 암호화하여 나이스 페이지 호출 시 POST 방식으로 넘겨줘야하는 값

        //iReturn 값에 따라 프로세스 진행여부 파악 -> 암호화 결과 코드
        int iReturn = niceCheck.fnEncode(sSiteCode, sSitePassword, sPlainData);
        if(iReturn == 0) {
            sEncData = niceCheck.getCipherData();   //암호화된 데이터(업체 정보 - 사이트코드 외 설정사항에 대한 정보)
        }else if (iReturn == -1){
            sMessage = "암호화 시스템 에러입니다.";
        }else if (iReturn == -2) {
            sMessage = "암호화 처리 오류입니다.";
        }else if (iReturn == -3){
            sMessage = "암호화 데이터 오류입니다.";
        }else if(iReturn == -9){
            sMessage = "입력 데이터 오류입니다.";
        }else {
            sMessage = "알 수 없는 에러입니다. iReturn : " + iReturn;
        }

        request.getSession().setAttribute("REQ_SEQ", sRequestNumber);

        modelMap.addAttribute("sMessage", sMessage);
        modelMap.addAttribute("sEncData", sEncData);

        return "pass_test";
    }

    @RequestMapping(value = "/pass/success", method = {RequestMethod.GET, RequestMethod.POST})
    public String passSuccess(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        CPClient niceCheck = new CPClient();

        String sEncodeData = request.getParameter("EncodeData");

        String sSiteCode = niceProperties.getSiteCode();        //Nice로부터 부여받은 사이트 코드
        String sSitePassword = niceProperties.getPassword();    //Nice로부터 부여받은 사이트 패스워드

        String sCipherTime = "";       //복호화한 시간
        String sRequestNumber = "";     //요청 번호
        String sResponseNumber = "";    //인증 고유번호
        String sAuthType = "";          //인증 수단
        String sMessage = "";
        String sPlainData = "";

        int iReturn = niceCheck.fnDecode(sSiteCode, sSitePassword, sEncodeData);

        if(iReturn == 0){
            sPlainData = niceCheck.getPlainData();
            sCipherTime = niceCheck.getCipherDateTime();

            //데이터를 추출한다.
            HashMap mapresult = niceCheck.fnParse(sPlainData);
            sRequestNumber = (String) mapresult.get("REQ_SEQ");
            sResponseNumber = (String) mapresult.get("RES_SEQ");
            sAuthType = (String) mapresult.get("AUTH_TYPE");

            String session_sRequestNumber = (String) request.getSession().getAttribute("REQ_SEQ");
            if(!sRequestNumber.equals(session_sRequestNumber)){
                sMessage = "세션값 불일치 오류입니다.";
            }

            PhoneAuthResultDto phoneAuthResultDto = PhoneAuthResultDto.builder()
                                                                      .sRequestNumber(sRequestNumber)
                                                                      .sResponseNumber(sResponseNumber)
                                                                      .sAuthType(sAuthType)
                                                                      .sCipherTime(sCipherTime)
                                                                      .name((String) mapresult.get("NAME"))
                                                                      .birth((String) mapresult.get("BIRTHDATE"))
                                                                      .gender((String) mapresult.get("GENDER"))
                                                                      .dupInfo((String) mapresult.get("DI"))
                                                                      .connInfo((String) mapresult.get("CI"))
                                                                      .phone((String) mapresult.get("MOBILE_NO"))
                                                                      .mobileCompany((String) mapresult.get("MOBILE_CO"))
                                                                      .build();

            modelMap.addAttribute("dto", phoneAuthResultDto);
        }
        else if (iReturn == -1) {
            sMessage = "복호화 시스템 오류입니다.";
        }
        else if (iReturn == -4) {
            sMessage = "복호화 처리 오류입니다.";
        }
        else if (iReturn == -5) {
            sMessage = "복호화 해쉬 오류입니다.";
        }
        else if (iReturn == -6) {
            sMessage = "복호화 데이터 오류입니다.";
        }
        else if (iReturn == -9) {
            sMessage = "입력 데이터 오류입니다.";
        }
        else if (iReturn == -12) {
            sMessage = "사이트 패스워드 오류입니다.";
        }else {
            sMessage = "알 수 없는 에러입니다. iReturn : " + iReturn;
        }
        modelMap.addAttribute("sMessage", sMessage);

        return "pass_success";
    }
}
