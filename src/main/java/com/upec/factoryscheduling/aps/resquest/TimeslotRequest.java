package com.upec.factoryscheduling.aps.resquest;

import lombok.Data;

@Data
public class TimeslotRequest {

    private String taskNo;
    private String produceId;
    private double time;
    private int slice;
}
