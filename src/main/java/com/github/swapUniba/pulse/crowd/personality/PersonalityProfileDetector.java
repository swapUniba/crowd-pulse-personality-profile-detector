package com.github.swapUniba.pulse.crowd.personality;

import com.github.frapontillo.pulse.crowd.data.entity.Message;
import com.github.frapontillo.pulse.crowd.data.entity.Personality;
import com.github.frapontillo.pulse.crowd.data.entity.Profile;
import com.github.frapontillo.pulse.crowd.data.repository.ProfileRepository;
import com.github.frapontillo.pulse.spi.IPlugin;
import com.github.frapontillo.pulse.spi.IPluginConfig;
import com.github.frapontillo.pulse.spi.PluginConfigHelper;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.Logger;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SafeSubscriber;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import static com.github.frapontillo.pulse.util.PulseLogger.getLogger;

/**
 * PersonalityProfileDetector plugin class.
 *
 * @author Cosimo Lovascio
 *
 */
public class PersonalityProfileDetector extends IPlugin<Message, Message, PersonalityProfileDetector.PersonalityProfileDetectorConfig> {

    private static final String PLUGIN_NAME = "personality-profile-detector";
    private final static Logger logger = getLogger(PersonalityProfileDetector.class);

    private ProfileRepository profileRepository;
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public PersonalityProfileDetectorConfig getNewParameter() {
        return new PersonalityProfileDetectorConfig();
    }

    @Override
    protected Operator<Message, Message> getOperator(PersonalityProfileDetectorConfig params) {

        // access to profiles database
        profileRepository = new ProfileRepository(params.getProfilesDatabaseName());

        return subscriber -> new SafeSubscriber<>(new Subscriber<Message>() {

            List<Message> allMessages = new ArrayList<>();

            @Override
            public void onCompleted() {
                Profile user = profileRepository.getByUsername(params.getUsername());
                if (user != null) {
                    List<Personality> userPersonalityList = user.getPersonalities();
                    if (userPersonalityList == null) {
                        userPersonalityList = new ArrayList<>();
                    }
                    userPersonalityList.add(calculatePersonality(allMessages));
                    logger.info("Personality calculated");

                    // update the user profile
                    Query<Profile> query = profileRepository.createQuery();
                    query.field("username").equal(user.getUsername());
                    UpdateOperations<Profile> update = profileRepository.createUpdateOperations();
                    update.set("personalities", userPersonalityList);
                    profileRepository.updateFirst(query, update);

                } else {
                    logger.info("No user profile found");
                }

                reportPluginAsCompleted();
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                reportPluginAsErrored();
                subscriber.onError(e);
            }

            @Override
            public void onNext(Message message) {
                allMessages.add(message);
                subscriber.onNext(message);
            }
        });
    }

    /**
     * Calculate personality based on all messages.
     * @param messages the messages
     * @return the user personality
     */
    private Personality calculatePersonality(List<Message> messages) {

        long timestamp = Calendar.getInstance().getTimeInMillis();

        String query = "http://90.147.170.25:8080/PersonalityEmpathy/rest/UserService/userPersonality";
        ArrayList<String> list = new ArrayList<String>();

        for (Message message : messages)
        {
            list.add(message.getText());
        }
        JSONObject json = new JSONObject();
        json.put("messages", new JSONArray(list));

        logger.info(json);
        URL url = new URL(query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes("UTF-8"));
        os.close();

        // read the response
        InputStream in = new BufferedInputStream(conn.getInputStream());
        String result = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

        JSONObject jsonObject = new JSONObject(result);

        String ext = jsonObject.getString("ext");
        String con = jsonObject.getString("con");
        String ope = jsonObject.getString("ope");
        String agr = jsonObject.getString("agr");
        String neu = jsonObject.getString("neu");

        Personality userPersonality = new Personality();
        userPersonality.setSource("personality-detector");
        userPersonality.setTimestamp(timestamp);

        userPersonality.setConfidence(Double.parseDouble(con));
        userPersonality.setAgreeableness(Double.parseDouble(agr));
        userPersonality.setNeuroticism(Double.parseDouble(neu));
        userPersonality.setConscientiousness(Double.parseDouble(con));
        userPersonality.setOpenness(Double.parseDouble(ope));
        userPersonality.setExtroversion(Double.parseDouble(ext));

        return userPersonality;
    }


    /**
     * PersonalityProfileDetector config class.
     */
    class PersonalityProfileDetectorConfig implements IPluginConfig<PersonalityProfileDetectorConfig> {
        private String profilesDatabaseName;
        private String username;

        @Override public PersonalityProfileDetectorConfig buildFromJsonElement(JsonElement json) {
            return PluginConfigHelper.buildFromJson(json, PersonalityProfileDetectorConfig.class);
        }

        public String getProfilesDatabaseName() {
            return profilesDatabaseName;
        }

        public void setProfilesDatabaseName(String profilesDatabaseName) {
            this.profilesDatabaseName = profilesDatabaseName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }



}