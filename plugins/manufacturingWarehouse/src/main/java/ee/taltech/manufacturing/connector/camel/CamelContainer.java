/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package ee.taltech.manufacturing.connector.camel;

import ee.taltech.manufacturing.connector.camel.routes.BaseRoute;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sparkrest.SparkComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultCamelContextNameStrategy;
import org.apache.camel.impl.DefaultPackageScanClassResolver;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.osgi.framework.ServiceRegistration;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

/**
 * A container for Apache Camel.
 */
public class CamelContainer implements Container {
    private static final String module = CamelContainer.class.getName();
    //    private static LocalDispatcherFactory dispatcherFactory;
    private static ProducerTemplate producerTemplate;
    private LocalDispatcher dispatcher;
    private CamelContext context;
    private String name;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        context = createCamelContext();
        context.setNameStrategy(new DefaultCamelContextNameStrategy("rest-api"));
        context.addComponent("restlet", new SparkComponent());
        ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(name, configFile);
        String packageName = ContainerConfig.getPropertyValue(cfg, "package", "ee.taltech.manufacturing.connector.camel.routes");
        PackageScanClassResolver packageResolver = new DefaultPackageScanClassResolver();
        Set<Class<?>> routesClassesSet = packageResolver.findImplementations(BaseRoute.class, packageName);

        routesClassesSet.stream()
                .filter(route -> !Modifier.isAbstract(route.getModifiers()))
                .forEach(key -> {
            try {
                Debug.logInfo("Creating route: " + key.getName(), module);
                RouteBuilder routeBuilder = createRoutes(key.getName());
                addRoutesToContext(routeBuilder);
            } catch (ContainerException e) {
                e.printStackTrace();
            }
        });
        producerTemplate = context.createProducerTemplate();
    }


    @Override
    public boolean start() throws ContainerException {
        Debug.logInfo("Starting camel container", module);

        try {
            context.start();
        } catch (Exception e) {
            throw new ContainerException(e);
        }
        return true;
    }

    @Override
    public void stop() throws ContainerException {
        Debug.logInfo("Stopping camel container", module);

        try {
            context.stop();
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public static ProducerTemplate getProducerTemplate() {
        if (producerTemplate == null) {
            throw new RuntimeException("ProducerTemplate not initialized");
        }
        return producerTemplate;
    }

    private void addRoutesToContext(RouteBuilder routeBuilder) throws ContainerException {
        try {
            context.addRoutes(routeBuilder);
        } catch (Exception e) {
            Debug.logError(e, "Cannot add routes: " + routeBuilder, module);
            throw new ContainerException(e);
        }
    }

    private DefaultCamelContext createCamelContext() throws ContainerException {
        dispatcher = createDispatcher();
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("dispatcher", dispatcher);
        return new DefaultCamelContext(registry);
    }

    private RouteBuilder createRoutes(String routeBuilderClassName) throws ContainerException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> c = loader.loadClass(routeBuilderClassName);
            return (RouteBuilder) c.getConstructor(LocalDispatcher.class).newInstance(dispatcher);
        } catch (Exception e) {
            Debug.logError(e, "Cannot get instance of the camel route builder: " + routeBuilderClassName, module);
            throw new ContainerException(e);
        }
    }

    private LocalDispatcher createDispatcher() throws ContainerException {
        Delegator delegator = DelegatorFactory.getDelegator("default");
        return ServiceContainer.getLocalDispatcher("camel-dispatcher", delegator);
        // return dispatcherFactory.createLocalDispatcher("camel-dispatcher", delegator);
    }
}
