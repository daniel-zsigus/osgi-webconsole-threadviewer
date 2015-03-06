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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.expression.ExpressionCompiler;
import org.everit.expression.ParserConfiguration;
import org.everit.expression.mvel.MvelExpressionCompiler;
import org.everit.templating.CompiledTemplate;
import org.everit.templating.TemplateCompiler;
import org.everit.templating.html.HTMLTemplateCompiler;
import org.everit.templating.text.TextTemplateCompiler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.metatype.MetaTypeProvider;

public class ThreadViewerServlet extends HttpServlet {

    private static final long serialVersionUID = -187515087668802084L;

    private final ClassLoader classLoader;

    private final CompiledTemplate componentsTemplate;

    public ThreadViewerServlet(final BundleContext bundleContext) {
        classLoader = bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();

        ExpressionCompiler expressionCompiler = new MvelExpressionCompiler();

        TextTemplateCompiler textTemplateCompiler = new TextTemplateCompiler(expressionCompiler);

        Map<String, TemplateCompiler> inlineCompilers = new HashMap<String, TemplateCompiler>();
        inlineCompilers.put("text", textTemplateCompiler);

        HTMLTemplateCompiler htmlTemplateCompiler = new HTMLTemplateCompiler(expressionCompiler, inlineCompilers);

        ParserConfiguration parserConfiguration = new ParserConfiguration(classLoader);

        Map<String, Class<?>> variableTypes = new HashMap<String, Class<?>>();
        variableTypes.put("mp", MetaTypeProvider.class);
        parserConfiguration.setVariableTypes(variableTypes);

        componentsTemplate = htmlTemplateCompiler.compile(readResource("META-INF/webcontent/threadviewer.html"),
                parserConfiguration);

    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {

        resp.setContentType("text/html");

        PrintWriter writer = resp.getWriter();

        String appRoot = (String) req.getAttribute("felix.webconsole.appRoot");
        String pluginRoot = (String) req.getAttribute("felix.webconsole.pluginRoot");

        String requestURI = req.getRequestURI();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("appRoot", appRoot);
        vars.put("pluginRoot", pluginRoot);

        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        List<Thread> threads = new ArrayList<Thread>(stackTraces.keySet());
        Collections.sort(threads, (t1, t2) -> (int) (t1.getId() - t2.getId()));
        vars.put("threads", threads);
        if (requestURI.equals(pluginRoot)) {
            componentsTemplate.render(writer, vars, "content");
        } else if (requestURI.replace(pluginRoot + "/", "").length() > 0) {
            try {
                long threadId = Long.parseLong(requestURI.replace(pluginRoot + "/", ""));
                Optional<Thread> thread = threads.stream().filter((t) -> t.getId() == threadId).findFirst();
                if (thread.isPresent()) {
                    vars.put("thread", thread.get());
                    componentsTemplate.render(writer, vars, "threadDetails");
                } else {
                    resp.sendRedirect(pluginRoot);
                    componentsTemplate.render(writer, vars, "content");
                }
            } catch (NumberFormatException e) {
                resp.sendRedirect(pluginRoot);
                componentsTemplate.render(writer, vars, "content");
            }
        } else {
            resp.setStatus(404);
            return;
        }
    }

    private String readResource(final String resourceName) {
        InputStream inputStream = classLoader.getResourceAsStream(resourceName);
        byte[] buf = new byte[1024];
        try {
            int r = inputStream.read(buf);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while (r > -1) {
                bout.write(buf, 0, r);
                r = inputStream.read(buf);
            }
            return new String(bout.toByteArray(), Charset.forName("UTF8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
