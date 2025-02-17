package uk.cam.mrc.phm.training.day2.fabiland.data;

import cern.colt.map.tfloat.OpenIntFloatHashMap;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.MitoGender;
import de.tum.bgu.msm.data.Mode;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.data.accessibility.Accessibility;
import de.tum.bgu.msm.data.accessibility.CommutingTimeProbability;
import de.tum.bgu.msm.data.dwelling.RealEstateDataManager;
import de.tum.bgu.msm.data.geo.GeoData;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.job.JobDataManager;
import de.tum.bgu.msm.data.person.Gender;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.health.HealthDataContainerImpl;
import de.tum.bgu.msm.health.HealthDiseaseTrackerWriter;
import de.tum.bgu.msm.health.HealthPersonWriter;
import de.tum.bgu.msm.health.data.DataContainerHealth;
import de.tum.bgu.msm.health.data.LinkInfo;
import de.tum.bgu.msm.health.disease.Diseases;
import de.tum.bgu.msm.health.disease.HealthExposures;
import de.tum.bgu.msm.io.NoiseDwellingWriter;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.schools.DataContainerWithSchools;
import de.tum.bgu.msm.schools.SchoolData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import uk.cam.mrc.phm.training.day2.fabiland.io.PersonWriterFabiland;

import java.util.*;

public class FabilandHealthDataContainer implements DataContainerHealth {
    private final static Logger logger = LogManager.getLogger(HealthDataContainerImpl.class);

    private final DataContainer delegate;
    private final Properties properties;
    private Map<Id<Link>, LinkInfo> linkInfo = new HashMap<>();
    private Set<Pollutant> pollutantSet = new HashSet<>();
    private Map<Zone, Map<Pollutant, OpenIntFloatHashMap>> zoneExposure2Pollutant2TimeBin = new HashMap<>();
    private EnumMap<Mode, EnumMap<MitoGender,Map<Integer,Double>>> avgSpeeds;
    private EnumMap<Diseases, Map<String, Double>> healthTransitionData;
    private EnumMap<HealthExposures, EnumMap<Diseases, TableDataSet>> doseResponseData;
    private Map<Integer, Map<Integer, List<String>>> healthDiseaseTrackerRemovedPerson = new HashMap<>();

    public FabilandHealthDataContainer(DataContainer delegate,
                                       Properties properties) {
        this.delegate = delegate;
        this.properties = properties;
    }

    @Override
    public HouseholdDataManager getHouseholdDataManager() {
        return delegate.getHouseholdDataManager();
    }

    @Override
    public RealEstateDataManager getRealEstateDataManager() {
        return delegate.getRealEstateDataManager();
    }

    @Override
    public JobDataManager getJobDataManager() {
        return delegate.getJobDataManager();
    }

    @Override
    public GeoData getGeoData() {
        return delegate.getGeoData();
    }

    @Override
    public TravelTimes getTravelTimes() {
        return delegate.getTravelTimes();
    }

    @Override
    public Accessibility getAccessibility() {
        return delegate.getAccessibility();
    }

    @Override
    public CommutingTimeProbability getCommutingTimeProbability() {
        return delegate.getCommutingTimeProbability();
    }

    @Override
    public void setup() {
        delegate.setup();
    }

    @Override
    public void prepareYear(int year) {
        delegate.prepareYear(year);
    }

    @Override
    public void endYear(int year) {
        delegate.endYear(year);
        if (year == properties.main.startYear || properties.healthData.exposureModelYears.contains(year)) {
            writePersonExposureData(year);
            writePersonRelativeRiskData(year);
        }
        writePersonDiseaseTrackData(year);
    }

    @Override
    public void endSimulation() {
        delegate.endSimulation();
    }

    private void writePersonDiseaseTrackData(int year) {
        final String outputDirectory = properties.main.baseDirectory + "scenOutput/" + properties.main.scenarioName + "/";
        String filepp = outputDirectory
                + properties.householdData.personFinalFileName
                + "_healthDiseaseTracker_"
                + year
                + ".csv";
        new HealthDiseaseTrackerWriter(this).writeHealthDiseaseTracking(filepp);
    }

    public void writePersonExposureData(int year) {
        final String outputDirectory = properties.main.baseDirectory + "scenOutput/" + properties.main.scenarioName + "/";
        String filepp = outputDirectory
                + properties.householdData.personFinalFileName
                + "_exposure_"
                + year
                + ".csv";
        new PersonWriterFabiland(this).writePersonExposure(filepp);
    }

    public void writePersonRelativeRiskData(int year) {
        final String outputDirectory = properties.main.baseDirectory + "scenOutput/" + properties.main.scenarioName + "/";
        String filepp = outputDirectory
                + properties.householdData.personFinalFileName
                + "_rr_"
                + year
                + ".csv";
        new PersonWriterFabiland(this).writePersonRelativeRisk(filepp);
    }

    private void writeDwellingsWithNoise(int year) {
        final String outputDirectory = properties.main.baseDirectory + "scenOutput/" + properties.main.scenarioName +"/";
        String filedd = outputDirectory
                + properties.realEstate.dwellingsFinalFileName
                + "Noise_"
                + year
                + ".csv";
        new NoiseDwellingWriter(delegate.getRealEstateDataManager()).writeDwellings(filedd);
    }

    @Override
    public Map<Id<Link>, LinkInfo> getLinkInfo() {
        return linkInfo;
    }

    @Override
    public void setLinkInfo(Map<Id<Link>, LinkInfo> linkInfo) {
        this.linkInfo = linkInfo;
    }

    @Override
    public Set<Pollutant> getPollutantSet() {
        return pollutantSet;
    }

    @Override
    public void setPollutantSet(Set<Pollutant> pollutantSet) {
        this.pollutantSet = pollutantSet;
    }

    @Override
    public EnumMap<Mode, EnumMap<MitoGender, Map<Integer, Double>>> getAvgSpeeds() {
        return avgSpeeds;
    }

    @Override
    public void setAvgSpeeds(EnumMap<Mode, EnumMap<MitoGender, Map<Integer, Double>>> avgSpeeds) {
        this.avgSpeeds = avgSpeeds;
    }

    public EnumMap<HealthExposures, EnumMap<Diseases, TableDataSet>> getDoseResponseData() {
        return doseResponseData;
    }

    public void setDoseResponseData(EnumMap<HealthExposures, EnumMap<Diseases, TableDataSet>> doseResponseData) {
        this.doseResponseData = doseResponseData;
    }

    @Override
    public void reset(){
        linkInfo.clear();
        zoneExposure2Pollutant2TimeBin.clear();
    }

    public Map<Integer, Map<Integer, List<String>>> getHealthDiseaseTrackerRemovedPerson() {
        return healthDiseaseTrackerRemovedPerson;
    }

    public Map<Zone, Map<Pollutant, OpenIntFloatHashMap>> getZoneExposure2Pollutant2TimeBin() {
        return zoneExposure2Pollutant2TimeBin;
    }

    public void setZoneExposure2Pollutant2TimeBin(Map<Zone, Map<Pollutant, OpenIntFloatHashMap>> zoneExposure2Pollutant2TimeBin) {
        this.zoneExposure2Pollutant2TimeBin = zoneExposure2Pollutant2TimeBin;
    }

    @Override
    public EnumMap<Diseases, Map<String, Double>> getHealthTransitionData() {
        return healthTransitionData;
    }
    @Override
    public void setHealthTransitionData(EnumMap<Diseases, Map<String, Double>> healthTransitionData) {
        this.healthTransitionData = healthTransitionData;
    }

    @Override
    public String createTransitionLookupIndex(int age, Gender gender, String location) {
        StringBuilder key = new StringBuilder();
        key.append(age).append("|").append(gender.name().toLowerCase()).append("|").append(location);
        return key.toString();
    }
}
