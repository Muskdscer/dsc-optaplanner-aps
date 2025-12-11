package com.upec.factoryscheduling.aps.resquest;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Data
public class ProcedureRequest {

    private String orderNo;
    private String machineNo;
    private Integer procedureNo;
    private LocalDateTime date;
}
