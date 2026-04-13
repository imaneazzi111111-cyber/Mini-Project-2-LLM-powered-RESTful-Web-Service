package com.aui.translator.bootstrap;

import com.aui.translator.api.TranslatorResource;
import com.aui.translator.error.GlobalExceptionMapper;
import com.aui.translator.filter.CorsResponseFilter;
import org.glassfish.jersey.server.ResourceConfig;

public class TranslatorApplication extends ResourceConfig {
    public TranslatorApplication() {
        register(TranslatorResource.class);
        register(CorsResponseFilter.class);
        register(GlobalExceptionMapper.class);
        packages("org.glassfish.jersey.jackson");
    }
}
