package com.community.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTicketDto {

    @NotNull
    @JsonProperty("ticket_state")
    private Long ticketState;

    @JsonProperty("ticket_status")
    private Long ticketStatus;

    @NotNull
    @JsonProperty("ticket_type")
    private Long ticketType;

    @NotNull
    @JsonProperty("assignee")
    private Long assignee;

    @NotNull
    @JsonProperty("assignee_role")
    private Integer assigneeRole;

    @NotNull
    @JsonProperty("target_completion_time")
    private Date targetCompletionDate;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("task")
    private String task;
}
