package com.kirela.szczepimy;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalTime;

public record TimeRange(
    @JsonSerialize(using = LocalTimeSerializer.class)
    LocalTime from,
    @JsonSerialize(using = LocalTimeSerializer.class)
    LocalTime to
) {
}
