package com.closetly.closetly_backend.config;

import com.closetly.closetly_backend.admin.entity.ReviewFlag;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToFlagStatusConverter implements Converter<String, ReviewFlag.FlagStatus> {

    @Override
    public ReviewFlag.FlagStatus convert(String source) {
        return ReviewFlag.FlagStatus.valueOf(source.toUpperCase());
    }
}
