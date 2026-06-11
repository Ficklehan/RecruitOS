package com.recruitos.agent.dto;

import lombok.Data;

@Data
public class RpaUnlockDTO {

    /** 必须为 true，防止误触开启真实平台访问 */
    private Boolean confirm;

    /** 联调原因，写入审计日志 */
    private String reason;
}
