package com.example.helloworld;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.example.helloworld.auth.ExampleAuthenticator;
import com.example.helloworld.cli.RenderCommand;
import com.example.helloworld.core.Template;
import com.example.helloworld.core.User;
import com.example.helloworld.db.UserDAO;
import com.example.helloworld.health.TemplateHealthCheck;
import com.example.helloworld.resources.AuthResource;
import com.example.helloworld.resources.ClientResource;
import com.example.helloworld.resources.UserResource;
import com.sun.jersey.api.client.Client;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
	public static void main(String[] args) throws Exception {
		new HelloWorldApplication().run(args);
	}

	private final HibernateBundle<HelloWorldConfiguration> hibernateBundle = new HibernateBundle<HelloWorldConfiguration>(
			User.class) {
		@Override
		public DataSourceFactory getDataSourceFactory(
				HelloWorldConfiguration configuration) {
			return configuration.getDataSourceFactory();
		}
	};

	@Override
	public String getName() {
		return "hello-world";
	}

	@Override
	public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
		bootstrap.addCommand(new RenderCommand());
		bootstrap.addBundle(new AssetsBundle());
		bootstrap.addBundle(new MigrationsBundle<HelloWorldConfiguration>() {
			@Override
			public DataSourceFactory getDataSourceFactory(
					HelloWorldConfiguration configuration) {
				return configuration.getDataSourceFactory();
			}
		});
		bootstrap.addBundle(hibernateBundle);
		
		bootstrap.addBundle(new AssetsBundle("/assets/app.js", "/app.js", null, "app"));
		bootstrap.addBundle(new AssetsBundle("/assets/stylesheets", "/stylesheets", null, "css"));
        bootstrap.addBundle(new AssetsBundle("/assets/directives", "/directives", null, "directives"));
        bootstrap.addBundle(new AssetsBundle("/assets/controllers", "/controllers", null, "controllers"));
        bootstrap.addBundle(new AssetsBundle("/assets/services", "/services", null, "services"));
        bootstrap.addBundle(new AssetsBundle("/assets/vendor", "/vendor", null, "vendor"));
        bootstrap.addBundle(new AssetsBundle("/assets/views", "/views", null, "views"));
	}

	@Override
    public void run(HelloWorldConfiguration configuration,
                    Environment environment) throws ClassNotFoundException {
        final Template template = configuration.buildTemplate();
        final UserDAO dao = new UserDAO(hibernateBundle.getSessionFactory());
        final Client client = new JerseyClientBuilder(environment).using(configuration.getJerseyClientConfiguration())
                .build(getName());

        environment.healthChecks().register("template", new TemplateHealthCheck(template));

        environment.jersey().register(new BasicAuthProvider<>(new ExampleAuthenticator(),
                                                              "SUPER SECRET STUFF"));
        
        environment.jersey().register(new ClientResource()); 
        environment.jersey().register(new UserResource(dao));
        environment.jersey().register(new AuthResource(client, dao));
    }
}
