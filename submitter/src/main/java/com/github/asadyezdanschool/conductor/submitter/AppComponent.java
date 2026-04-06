package com.github.asadyezdanschool.conductor.submitter;

import dagger.Component;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    ResourceConfig resourceConfig();
    DataSource dataSource();
}