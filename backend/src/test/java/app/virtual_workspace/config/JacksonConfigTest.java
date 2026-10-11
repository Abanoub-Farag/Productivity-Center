package app.virtual_workspace.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import java.util.TimeZone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class JacksonConfigTest {

    private final JacksonConfig config = new JacksonConfig();
    private final ObjectMapper mapper = config.objectMapper();

    static class TimePayload {
        private Instant timestamp;
        private LocalDate date;

        public TimePayload() {}
        public TimePayload(Instant timestamp, LocalDate date) {
            this.timestamp = timestamp;
            this.date = date;
        }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    @Test
    @DisplayName("Should serialize Instant as ISO-8601 string, not as epoch timestamp")
    void shouldSerializeInstantAsIsoString() throws Exception {
        Instant instant = Instant.parse("2026-10-11T12:00:00Z");
        LocalDate date = LocalDate.of(2026, 10, 11);
        TimePayload payload = new TimePayload(instant, date);

        String json = mapper.writeValueAsString(payload);

        assertThat(json).contains("\"timestamp\":\"2026-10-11T12:00:00Z\"");
        assertThat(json).contains("\"date\":\"2026-10-11\"");
    }

    @Test
    @DisplayName("Should deserialize ISO-8601 string to Instant")
    void shouldDeserializeIsoStringToInstant() throws Exception {
        String json = "{\"timestamp\":\"2026-10-11T12:00:00Z\",\"date\":\"2026-10-11\"}";

        TimePayload payload = mapper.readValue(json, TimePayload.class);

        assertThat(payload.getTimestamp()).isEqualTo(Instant.parse("2026-10-11T12:00:00Z"));
        assertThat(payload.getDate()).isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @Test
    @DisplayName("Should serialize TimerSessionResponseDto timestamps as ISO-8601 strings")
    void shouldSerializeTimerSessionResponseDtoTimestampsAsIsoString() throws Exception {
        app.virtual_workspace.timer.dtos.TimerSessionResponseDto dto = app.virtual_workspace.timer.dtos.TimerSessionResponseDto.builder()
                .id(1L)
                .userId(2L)
                .roomId(3L)
                .startedAt(Instant.parse("2026-10-11T14:30:00Z"))
                .endedAt(Instant.parse("2026-10-11T14:55:00Z"))
                .duration(1500L)
                .status(app.virtual_workspace.timer.models.enums.TimerStatus.DONE)
                .build();

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"startedAt\":\"2026-10-11T14:30:00Z\"");
        assertThat(json).contains("\"endedAt\":\"2026-10-11T14:55:00Z\"");
    }

    @Test
    @DisplayName("Should serialize Analytics DTOs dates and timestamps as ISO strings")
    void shouldSerializeAnalyticsDtosAsIsoStrings() throws Exception {
        app.virtual_workspace.timer.dtos.analytics.DailyFocusDto daily =
                new app.virtual_workspace.timer.dtos.analytics.DailyFocusDto(LocalDate.of(2026, 10, 11), 3600L);
        app.virtual_workspace.timer.dtos.analytics.WeeklyFocusDto weekly =
                new app.virtual_workspace.timer.dtos.analytics.WeeklyFocusDto(LocalDate.of(2026, 10, 5), 18000L);
        app.virtual_workspace.timer.dtos.analytics.MonthlyFocusDto monthly =
                new app.virtual_workspace.timer.dtos.analytics.MonthlyFocusDto(LocalDate.of(2026, 10, 1), 72000L);
        app.virtual_workspace.tasks.dtos.analytics.DailyTaskCountDto dailyTasks =
                new app.virtual_workspace.tasks.dtos.analytics.DailyTaskCountDto(LocalDate.of(2026, 10, 11), 5L, 3L);
        app.virtual_workspace.tasks.dtos.analytics.OldestPendingTaskDto oldestTask =
                new app.virtual_workspace.tasks.dtos.analytics.OldestPendingTaskDto(10L, "Old Task", Instant.parse("2026-10-01T08:00:00Z"));

        assertThat(mapper.writeValueAsString(daily)).contains("\"date\":\"2026-10-11\"");
        assertThat(mapper.writeValueAsString(weekly)).contains("\"weekStart\":\"2026-10-05\"");
        assertThat(mapper.writeValueAsString(monthly)).contains("\"monthStart\":\"2026-10-01\"");
        assertThat(mapper.writeValueAsString(dailyTasks)).contains("\"date\":\"2026-10-11\"");
        assertThat(mapper.writeValueAsString(oldestTask)).contains("\"createdAt\":\"2026-10-01T08:00:00Z\"");
    }

    @Test
    @DisplayName("Should serialize TimerSessionResponseDto with Spring Boot 4 JsonMapper as ISO-8601 strings")
    void shouldSerializeWithSpringBoot4JsonMapper() throws Exception {
        tools.jackson.databind.json.JsonMapper jsonMapper = tools.jackson.databind.json.JsonMapper.builder()
                .configure(tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .defaultTimeZone(TimeZone.getTimeZone("UTC"))
                .build();

        app.virtual_workspace.timer.dtos.TimerSessionResponseDto dto = app.virtual_workspace.timer.dtos.TimerSessionResponseDto.builder()
                .id(1L)
                .userId(2L)
                .roomId(3L)
                .startedAt(Instant.parse("2026-10-11T14:30:00Z"))
                .endedAt(Instant.parse("2026-10-11T14:55:00Z"))
                .duration(1500L)
                .status(app.virtual_workspace.timer.models.enums.TimerStatus.DONE)
                .build();

        String json = jsonMapper.writeValueAsString(dto);

        assertThat(json).contains("\"startedAt\":\"2026-10-11T14:30:00Z\"");
        assertThat(json).contains("\"endedAt\":\"2026-10-11T14:55:00Z\"");
    }

    @Test
    @DisplayName("Should bind Spring Boot 4 Jackson datetime property without error")
    void shouldBindSpringBoot4JacksonProperties() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withPropertyValues(
                        "spring.jackson.datatype.datetime.write-dates-as-timestamps=false",
                        "spring.jackson.time-zone=UTC"
                )
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(tools.jackson.databind.json.JsonMapper.class);
                });
    }
}
