package com.github.asadyezdanschool.conductor.submitter;

import com.github.asadyezdanschool.conductor.submitter.grpc.SchedulerGrpcClient;
import dagger.Component;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    ResourceConfig resourceConfig();
    SchedulerGrpcClient schedulerGrpcClient();
}
