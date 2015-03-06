/*
 * Copyright (C) 2015 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.org.everit.osgi.webconsole.threadviewer;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ThreadViewerActivator implements BundleActivator {

    private ServiceRegistration<Servlet> servletSR;

    @Override
    public void start(final BundleContext context) {
        Hashtable<String, String> servletProps = new Hashtable<String, String>();
        servletProps.put("felix.webconsole.label", "everit_threadviewer");
        servletProps.put("felix.webconsole.category", "Everit");
        servletProps.put("felix.webconsole.title", "Threadviewer");
        // servletProps.put("felix.webconsole.css", "res/ui/config.css");

        Servlet servlet = new ThreadViewerServlet(context);
        servletSR = context.registerService(Servlet.class, servlet, servletProps);
    }

    @Override
    public void stop(final BundleContext context) {
        servletSR.unregister();
    }

}
