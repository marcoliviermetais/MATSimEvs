package org.matsim.project;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEvExample {
    //Original EV run from matsim contribs


    static final String DEFAULT_CONFIG_FILE = "test/input/org/matsim/contrib/ev/example/RunEvExample/config.xml";
    private static final Logger log = Logger.getLogger(org.matsim.contrib.ev.example.RunEvExample.class);

    public static void main(String[] args) throws IOException {
        final URL configUrl;
        if (args.length > 0) {
            log.info("Starting simulation run with the following arguments:");
            configUrl = new URL(args[0]);
            log.info("config URL: " + configUrl);
        } else {
            File localConfigFile = new File(DEFAULT_CONFIG_FILE);
            if (localConfigFile.exists()) {
                log.info("Starting simulation run with the local example config file");
                configUrl = localConfigFile.toURI().toURL();
            } else {
                log.info("Starting simulation run with the example config file from GitHub repository");
                configUrl = new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/contribs/ev/"
                        + DEFAULT_CONFIG_FILE);
            }
        }
        new org.matsim.contrib.ev.example.RunEvExample().run(configUrl);
    }

    public void run(URL configUrl) {
        Config config = ConfigUtils.loadConfig(configUrl, new EvConfigGroup());

        config.controler()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new EvModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addRoutingModuleBinding(TransportMode.car).toProvider(new EvNetworkRoutingProvider(TransportMode.car));
                installQSimModule(new AbstractQSimModule() {
                    @Override
                    protected void configureQSim() {
                        bind(VehicleChargingHandler.class).asEagerSingleton();
                        bind(MobsimScopeEventHandler.class).to(VehicleChargingHandler.class);
                    }
                });
            }
        });
        controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));


        controler.run();
    }
}
