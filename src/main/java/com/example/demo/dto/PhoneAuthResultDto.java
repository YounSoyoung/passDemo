package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhoneAuthResultDto {
    String sRequestNumber;
    String sResponseNumber;
    String sAuthType;
    String sCipherTime;
    String name;
    String birth;
    String gender;
    String dupInfo;
    String connInfo;
    String phone;
    String mobileCompany;
    String sErrorCode;
}
