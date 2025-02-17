package uk.cam.mrc.phm.training.day2.fabiland.runHealth;

import de.tum.bgu.msm.SiloModel;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.container.ModelContainer;
import de.tum.bgu.msm.health.HealthDataContainerImpl;
import de.tum.bgu.msm.io.output.DefaultResultsMonitor;
import de.tum.bgu.msm.io.output.HouseholdSatisfactionMonitor;
import de.tum.bgu.msm.io.output.MultiFileResultsMonitor;
import de.tum.bgu.msm.io.output.ResultsMonitor;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import uk.cam.mrc.phm.training.day2.fabiland.data.FabilandHealthDataContainer;
import uk.cam.mrc.phm.training.day2.fabiland.run.DataBuilderFabiland;
import uk.cam.mrc.phm.training.day2.fabiland.run.ModelBuilderFabiland;

import java.util.Arrays;

public class RunFabilandHealth {

    private final static Logger logger = LogManager.getLogger(RunFabilandHealth.class);

    public static void main(String[] args) {
        Properties properties = SiloUtil.siloInitialization(args[0]);

        String[] matsimArgs = Arrays.copyOfRange( args, 1, args.length );

        Config config = ConfigUtils.loadConfig(matsimArgs);
        logger.info("Started SILO Fabiland sandbox model");

        // The following is obviously just a dirty quickfix until access/egress is default in MATSim
        if (properties.transportModel.includeAccessEgress) {
           config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
        }

        FabilandHealthDataContainer dataContainer =DataBuilderFabilandHealth.buildDataContainer(properties, config);
        DataBuilderFabilandHealth.readInput(properties, dataContainer);

        ModelContainer modelContainer = ModelBuilderFabilandHealth.getModelContainer(dataContainer, properties, config);

        ResultsMonitor resultsMonitor = new DefaultResultsMonitor(dataContainer, properties);
        MultiFileResultsMonitor multiFileResultsMonitor = new MultiFileResultsMonitor(dataContainer, properties);
        HouseholdSatisfactionMonitor householdSatisfactionMonitor = new HouseholdSatisfactionMonitor(dataContainer, properties, modelContainer);

        SiloModel model = new SiloModel(properties, dataContainer, modelContainer);
        model.addResultMonitor(resultsMonitor);
        model.addResultMonitor(multiFileResultsMonitor);
        model.addResultMonitor(householdSatisfactionMonitor);
        model.runModel();
        logger.info("Finished SILO.");
    }
}
