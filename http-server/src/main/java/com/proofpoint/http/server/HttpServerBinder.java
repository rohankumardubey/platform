package com.proofpoint.http.server;

import com.google.inject.Binder;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class HttpServerBinder
{
    private final Binder binder;

    private HttpServerBinder(Binder binder)
    {
        this.binder = Objects.requireNonNull(binder, "binder is null").skipSources(getClass());
    }

    public static HttpServerBinder httpServerBinder(Binder binder)
    {
        return new HttpServerBinder(binder);
    }

    public HttpResourceBinding bindResource(String baseUri, String classPathResourceBase)
    {
        return bindResource(baseUri, classPathResourceBase, TheServlet.class);
    }

    private HttpResourceBinding bindResource(String baseUri,
            String classPathResourceBase,
            Class<? extends Annotation> annotationType)
    {
        HttpResourceBinding httpResourceBinding = new HttpResourceBinding(baseUri, classPathResourceBase);
        newSetBinder(binder, HttpResourceBinding.class, annotationType).addBinding().toInstance(httpResourceBinding);
        return httpResourceBinding;
    }

    public static class HttpResourceBinding
    {
        private final String baseUri;
        private final String classPathResourceBase;
        private final List<String> welcomeFiles = new ArrayList<>();

        public HttpResourceBinding(String baseUri, String classPathResourceBase)
        {
            this.baseUri = baseUri;
            this.classPathResourceBase = classPathResourceBase;
        }

        public String getBaseUri()
        {
            return baseUri;
        }

        public String getClassPathResourceBase()
        {
            return classPathResourceBase;
        }

        public List<String> getWelcomeFiles()
        {
            return List.copyOf(welcomeFiles);
        }

        public HttpResourceBinding withWelcomeFile(String welcomeFile)
        {
            welcomeFiles.add(welcomeFile);
            return this;
        }
    }
}
