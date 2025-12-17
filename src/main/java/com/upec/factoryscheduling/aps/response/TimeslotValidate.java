package com.upec.factoryscheduling.aps.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class TimeslotValidate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String timeslotId;
    private String taskNo;
    private String procedureId;
    private String message;
    private int code;
}
