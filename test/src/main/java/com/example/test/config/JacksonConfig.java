package com.example.test.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置：日期时间序列化为 yyyy-MM-dd HH:mm:ss，
 * 与 Express 侧 mysql2 dateStrings（去毫秒）的输出格式保持一致
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(dateTime), new LocalDateSerializer(date))
                .deserializers(new LocalDateTimeDeserializer(dateTime), new LocalDateDeserializer(date));
    }
}
