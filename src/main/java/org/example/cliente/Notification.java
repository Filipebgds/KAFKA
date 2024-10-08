package org.example.cliente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    private String type;
    private Integer priority;
    private String message;


    public Integer getPriority() {
        return priority;
    }


    public String getMessage() {
        return message;
    }
}

