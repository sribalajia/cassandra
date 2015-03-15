import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author : asribalaji
 */
public class TwitterDemo {
    public static void main(String[] args) throws TwitterException {

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("***")
                .setOAuthConsumerSecret("***")
                .setOAuthAccessToken("***")
                .setOAuthAccessTokenSecret("***");
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();

        System.out.println(twitter.toString());
        /*
        // Latest tweets
        int count=0;
        for (int i=10; i <= 1000 ; i++) {
            List<Status> statusList = twitter.getHomeTimeline(new Paging(i,50));
            for (Status status : statusList) {
                System.out.println(status.toString());
                insertTweet("tweets", status);
                System.out.println(count);
            }
        }
        */

        //System.out.println(count);
        /*
        // Friend's IDs
        int frCount=0;
        IDs ids = twitter.getFriendsIDs(twitter.getId(), -1);
        List<String> friendsList = new ArrayList<String>();
        try {
            for (long id : ids.getIDs()) {
                User user = twitter.showUser(id);
                friendsList.add(user.getScreenName());
                //System.out.println(user.getScreenName());
                frCount++;
            }
        }catch (Exception e) {
            System.out.println(e);
            System.out.println(frCount);
            insertFriends(twitter.showUser(twitter.getId()), friendsList);
        }
        */
        int foCount=0;
        IDs ids = twitter.getFollowersIDs(twitter.getId(), -1);
        List<String> followersList = new ArrayList<String>();
        for (long id : ids.getIDs()) {
            User user = twitter.showUser(id);
            //insertUser(user);
            followersList.add(user.getScreenName());
            foCount++;
        }
        System.out.println(foCount);
        insertFollowers(twitter.showUser(twitter.getId()), followersList);
        //User details
        User user = twitter.showUser(twitter.getId());
        System.out.println();
        //insertUser(user);

    }

    private static void insertTweet(String tweets, Status status) {
        Map<String, String> tweetMap = getTweetMap(status);
        insert("tweets", String.valueOf(status.getId()), tweetMap);
    }

    private static Map<String, String> getTweetMap(Status status) {
        Map<String, String> userMap = new HashMap<String, String>();
        //userMap.put("screenName", user.getScreenName());
        userMap.put("text", status.getText());
        userMap.put("createdAt", String.valueOf(status.getCreatedAt().getTime()));
        userMap.put("screenName", status.getUser().getScreenName());
        return userMap;
    }

    private static Keyspace getKeySpace() {
        try {
            CassandraHostConfigurator configurator = new CassandraHostConfigurator("127.0.0.1:9160");
            Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", configurator);
            Keyspace keyspaceObj = HFactory.createKeyspace("demo", cluster);
            configurator.setMaxActive(2);
            return keyspaceObj;
        }catch (Exception e) {
            throw new HectorException(e);
        }
    }

    public static void insertFriends(User user,List<String> frList) {
        Map<String, String> frMap = new HashMap<String, String>();
        frMap.put("friends", String.valueOf(frList));
        insert("friends_list", user.getScreenName(), frMap);
    }

    public static void insertFollowers(User user,List<String> foList) {
        Map<String, String> foMap = new HashMap<String, String>();
        foMap.put("followers", String.valueOf(foList));
        insert("followers_list", user.getScreenName(), foMap);
    }

    public static void insertUser(User user) {
        Map<String, String> valueMap = getUserMap(user);
        insert("users", user.getScreenName(), valueMap);
    }


    public static void insert(String columnFamily, String key, Map<String, String> valueMap) {
        ColumnFamilyTemplate<String, String> template = new ThriftColumnFamilyTemplate<String, String>(getKeySpace(), columnFamily,
                StringSerializer.get(), StringSerializer.get());
        ColumnFamilyUpdater<String, String> updater = template.createUpdater(key);
        try {
            updateUpdater(updater,valueMap);
            template.update(updater);
        } catch (HectorException e) {
            throw new HectorException(e);
        } finally {
        }

    }

    private static Map<String, String> getUserMap(User user) {
        Map<String, String> userMap = new HashMap<String, String>();
        //userMap.put("screenName", user.getScreenName());
        userMap.put("user_name", user.getName());
        userMap.put("location", user.getLocation());
        userMap.put("id", String.valueOf(user.getId()));
        userMap.put("createdAt", String.valueOf(user.getCreatedAt().getTime()));
        return userMap;
    }

    private static void updateUpdater(ColumnFamilyUpdater updater, Map<String, String> valueMap) {
        int ttlValue = -1;
        if (updater != null) {
            for (String key : valueMap.keySet()) {
                if (key.equals("id") || key.equals("createdAt")) {
                    updater.setLong(key, Long.parseLong(valueMap.get(key)));
                } else {
                    updater.setString(key, valueMap.get(key));
                }
            }
        }
    }

}
