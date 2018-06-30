package com.graphhopper.http;

import com.graphhopper.util.CmdArgs;

import javax.inject.Singleton;

public class GraphHopperServletModuleIndoor extends GraphHopperServletModule {

    public GraphHopperServletModuleIndoor(CmdArgs args){
        super(args);
    }

    @Override
    protected void configureServlets() {
        filter("*").through(HeadFilter.class);
        bind(HeadFilter.class).in(Singleton.class);

        filter("*").through(CORSFilter.class, params);
        bind(CORSFilter.class).in(Singleton.class);

        filter("*").through(IPFilter.class);
        bind(IPFilter.class).toInstance(new IPFilter(args.get("jetty.whiteips", ""), args.get("jetty.blackips", "")));

        serve("/i18n*").with(I18NServlet.class);
        bind(I18NServlet.class).in(Singleton.class);

        serve("/info*").with(InfoServlet.class);
        bind(InfoServlet.class).in(Singleton.class);

        serve("/route*").with(GraphHopperServletIndoor.class);
        bind(GraphHopperServletIndoor.class).in(Singleton.class);

        serve("/nearest*").with(NearestServlet.class);
        bind(NearestServlet.class).in(Singleton.class);

        if (args.getBool("web.change_graph.enabled", false)) {
            serve("/change*").with(ChangeGraphServlet.class);
            bind(ChangeGraphServlet.class).in(Singleton.class);
        }

        // Can't do this because otherwise we can't add more paths _after_ this module.
        // Instead, put this route explicitly into Jetty.
        // (We really need a web service framework.)
        // serve("/*").with(InvalidRequestServlet.class);
        bind(InvalidRequestServlet.class).in(Singleton.class);
    }
}
