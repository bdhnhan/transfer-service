package com.zalopay.transfer.controller;

import com.zalopay.transfer.controller.request.CallbackRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/transfer")
@Slf4j
public class TransferController {

    @PostMapping("/callback")
    public String callBackTransaction(@RequestBody CallbackRequest request) {
        log.info("Receive request :: {}", request);
        return "OK";
    }
}
