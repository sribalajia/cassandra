package demo;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.StringKeyIterator;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.commons.lang.time.DateUtils;
import twitter4j.Status;
import twitter4j.TwitterException;

import javax.xml.bind.DatatypeConverter;
import java.security.SecureRandom;
import java.util.*;

/**
 * Author : asribalaji
 */
public class TwitterMain {

    private static final String TWEETS_CF = "tweets";
    private static final String USERS_CF = "users";

    private static final String USER_FOLLOWERS_CF = "user_followers";
    private static final String USER_FRIENDS_CF = "user_friends";

    private static final String USER_TWEETS_DAY_CF = "user_tweets_day";
    private static final String USER_TWEETS_MONTH_CF = "user_tweets_month";
    private static final String USER_TWEETS_HOUR_CF = "user_tweets_hour";

    private static final String USER_COUNT_INFO_CF = "user_count_info";
    private static final String SCREEN_NAME = "asribalaji";
    private static final String FOLLOWERS = "followers";
    private static final String FRIENDS = "friends";
    private static final String TWEETS = "tweets";


    public static void main(String[] args) throws TwitterException, InterruptedException {

        // Store tweets for user
        //storeTweetsForTime(USER_TWEETS_MONTH_CF, Calendar.MONTH);
        //storeTweetsForTime(USER_TWEETS_DAY_CF, Calendar.DAY_OF_MONTH);
        //storeTweetsForTime(USER_TWEETS_MONTH_CF, Calendar.HOUR);

        getAllTweetsForUser("Forbes");
        getTweetForUsersPerTimeline("Forbes", 0, 0, 12);
        getHomeTimeline(SCREEN_NAME);
        getFriendsForUser(SCREEN_NAME);
        getFollowersForUser(SCREEN_NAME);
        System.out.println(getFriendsCountForUser(SCREEN_NAME));
        System.out.println(getFollowersCountUser(SCREEN_NAME));
    }

    private static void getTweetForUsersPerTimeline(String screenName, int month, int day, int hour) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        List<String> users = new ArrayList<String>();
        users.add(screenName);
        List<String> tweetIds = getTweetIdsForUsersPerTimeline(users, month, day, hour);
        for (String key: tweetIds) {
            Tweet tweet = Tweet.convertMaptoTweet(CassandraUtil.getInstance().get(TWEETS_CF, key));
            System.out.println(tweet);
        }
    }

    private static List<Tweet> getAllTweetsForUser(String screenName) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        Map<String, String> columnMap = new HashMap<String, String>();
        columnMap.put("screen_name", screenName);
        List<String> tweetIds = CassandraUtil.getInstance().searchForKeysWithSearchMap(TWEETS_CF, columnMap, null, null, 0);
        for (String key: tweetIds) {
            Tweet tweet = Tweet.convertMaptoTweet(CassandraUtil.getInstance().get(TWEETS_CF, key));
            tweets.add(tweet);
            System.out.println(tweet);
        }
        return tweets;
    }

    private static void getHomeTimeline(String screenName) {
        List<String> friends = getFriendIdsForUser(screenName);
        List<Tweet> tweets = getTweetsForIds(getTweetIdsForUsers(friends));
    }

    private static List<Tweet> getTweetsForIds(List<String> ids) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        for (String id : ids) {
            Tweet tweet = Tweet.convertMaptoTweet(CassandraUtil.getInstance().get(TWEETS_CF, id));
            tweets.add(tweet);
            System.out.println(tweet);
        }
        return tweets;
    }

    private static void getFriendsForUser(String screenName) {
        List<TwitterUser> friends = new ArrayList<TwitterUser>();
        List<String> friendIds =  getFriendIdsForUser(screenName);
        for (String name : friendIds) {
            Map<String, String> userMap = CassandraUtil.getInstance().get(USERS_CF, name);
            if (userMap != null && userMap.size() > 0) {
                TwitterUser user = TwitterUser.convertMaptoUser(userMap);
                friends.add(user);
            } else {
                TwitterUser newUser = new TwitterUser();
                newUser.setScreenName(name);
                newUser.setUserName(name);
                newUser.setLocation("bangalore");
                newUser.setCreatedAt(new Date());
                newUser.setId(String.valueOf(generateRandId()));
                insertUser(newUser);
                friends.add(newUser);
            }
        }
        for (TwitterUser user : friends) {
            //demo.CassandraUtil.getInstance().incrementCounter(USER_COUNT_INFO_CF, screenName, "friends", 1);
            System.out.println(user);
        }
    }

    private static void getFollowersForUser(String screenName) {
        List<TwitterUser> followers = new ArrayList<TwitterUser>();
        List<String> followerIds =  getFollowerIdsForUser(screenName);
        for (String name : followerIds) {
            Map<String, String> userMap = CassandraUtil.getInstance().get(USERS_CF, name);
            if (userMap != null && userMap.size() > 0) {
                TwitterUser user = TwitterUser.convertMaptoUser(userMap);
                followers.add(user);
            } else {
                TwitterUser newUser = new TwitterUser();
                newUser.setScreenName(name);
                newUser.setUserName(name);
                newUser.setLocation(generateRandText(8));
                newUser.setCreatedAt(new Date());
                newUser.setId(String.valueOf(generateRandId()));
                insertUser(newUser);
                followers.add(newUser);
            }
        }
        for (TwitterUser user : followers) {
            //demo.CassandraUtil.getInstance().incrementCounter(USER_COUNT_INFO_CF, screenName, FOLLOWERS, 1);
            System.out.println(user);
        }
    }

    private static long getFriendsCountForUser(String screenName) {
        return CassandraUtil.getInstance().getCounterValue(USER_COUNT_INFO_CF, screenName, FRIENDS);
    }

    private static long getFollowersCountUser(String screenName) {
        return CassandraUtil.getInstance().getCounterValue(USER_COUNT_INFO_CF, screenName, FOLLOWERS);
    }

    private static long getTweetsCountForUser(String screenName) {
        return CassandraUtil.getInstance().getCounterValue(USER_COUNT_INFO_CF, screenName, TWEETS);
    }

    private static List<String> getFriendIdsForUser(String screenName) {
        List<String> friendIds = new ArrayList<String>();
        Map<String, String> friendsMap = CassandraUtil.getInstance().get(USER_FRIENDS_CF, screenName);
        for (String key: friendsMap.keySet()) {
            friendIds.add(key);
        }
        return friendIds;
    }

    private static List<String> getFollowerIdsForUser(String screenName) {
        List<String> followerIds = new ArrayList<String>();
        Map<String, String> followersMap = CassandraUtil.getInstance().get(USER_FOLLOWERS_CF, screenName);
        for (String key: followersMap.keySet()) {
            followerIds.add(key);
        }
        return followerIds;
    }

    private static List<String> getTweetIdsForUsers(List<String> users) {
        final MultigetSliceQuery<String, Composite, String> query = HFactory.createMultigetSliceQuery(CassandraUtil.getInstance().getKeySpace(),
                StringSerializer.get(),
                CompositeSerializer.get(),
                StringSerializer.get());

        query.setColumnFamily(USER_TWEETS_DAY_CF);

        Date startTime = new Date(115,2,8);
        Date endTime = new Date(115,2,9);
        //String key = user + "@@@" + startTime;
        List<String> rowKeys = new ArrayList<String>();
        for (String user: users) {
            rowKeys.add(user + "@@@" + startTime);
        }
        query.setKeys(rowKeys);
        startTime.setHours(20);
        query.setRange(new Composite(endTime), new Composite(startTime), true, Integer.MAX_VALUE);

        final QueryResult<Rows<String,Composite,String>> queryResult = query.execute();
        final Rows<String,Composite,String> rows = queryResult.get();
        List<String> resultList = new ArrayList<String>();
        for (Row<String, Composite, String> currentRow : rows ) {
            if (currentRow.getColumnSlice().getColumns() != null && currentRow.getColumnSlice().getColumns().size() > 0) {
                Map<String, String> currentColumnMap = new HashMap<String, String>();
                for (HColumn<Composite, String> currentColumn : currentRow.getColumnSlice().getColumns()) {
                    String value = new String(currentColumn.getValue());
                    currentColumnMap.put(String.valueOf(currentColumn.getName()), value);
                    resultList.add(value);
                }
            }
        }
        return resultList;
    }

    private static List<String> getTweetIdsForUsersPerTimeline(List<String> users, int month, int date, int hour) {
        final MultigetSliceQuery<String, Composite, String> query = HFactory.createMultigetSliceQuery(CassandraUtil.getInstance().getKeySpace(),
                StringSerializer.get(),
                CompositeSerializer.get(),
                StringSerializer.get());

        String columnFamily = null;
        Date curTime = new Date();
        curTime.setDate(8);
        curTime.setHours(23);

        int sMonth, eMonth, sDate, eDate, sHour, eHour;
        if (month == 0)
            sMonth = eMonth = curTime.getMonth();
        else {
            eMonth = curTime.getMonth();
            sMonth = eMonth - month;
        }

        if (date == 0)
            sDate = eDate = curTime.getDate();
        else {
            eDate = curTime.getDay();
            sDate = eDate - date;
            columnFamily = USER_TWEETS_MONTH_CF;
        }

        if (hour == 0)
            sHour = eHour = curTime.getHours();
        else {
            eHour = curTime.getHours();
            sHour = eHour - hour;
            columnFamily = USER_TWEETS_DAY_CF;
        }

        query.setColumnFamily(columnFamily);

        Date startTime = new Date(115,sMonth,sDate);
        Date endTime = new Date(115,eMonth,eDate);

        List<String> rowKeys = new ArrayList<String>();
        for (String user: users) {
            rowKeys.add(user + "@@@" + startTime);
        }
        query.setKeys(rowKeys);
        startTime.setHours(sHour);
        endTime.setHours(eHour);
        query.setRange(new Composite(endTime), new Composite(startTime), true, Integer.MAX_VALUE);

        final QueryResult<Rows<String,Composite,String>> queryResult = query.execute();
        final Rows<String,Composite,String> rows = queryResult.get();
        List<String> resultList = new ArrayList<String>();
        for (Row<String, Composite, String> currentRow : rows ) {
            if (currentRow.getColumnSlice().getColumns() != null && currentRow.getColumnSlice().getColumns().size() > 0) {
                Map<String, String> currentColumnMap = new HashMap<String, String>();
                for (HColumn<Composite, String> currentColumn : currentRow.getColumnSlice().getColumns()) {
                    String value = new String(currentColumn.getValue());
                    currentColumnMap.put(String.valueOf(currentColumn.getName()), value);
                    resultList.add(value);
                }
            }
        }
        return resultList;
    }

    private static void storeTweetsForTime(String columnFamily, int calenderOption) {
        StringKeyIterator sk = new StringKeyIterator(CassandraUtil.getInstance().getKeySpace(), TWEETS_CF);
        Iterator<String> iterator = sk.iterator();
        List<String> resultList = new ArrayList<String>();
        while (iterator.hasNext()) {
            resultList.add(iterator.next());
        }
        if (resultList.size() > 0) {
            for (String key: resultList) {
                Map<String, String> tweetMap = CassandraUtil.getInstance().get(TWEETS_CF, key);
                Tweet tweet = Tweet.convertMaptoTweet(tweetMap);
                Composite composite = new Composite();
                composite.add(Long.parseLong(tweet.getCreatedAt()));
                Date createdAt = new Date(Long.parseLong(tweet.getCreatedAt()));
                String rowKey = tweet.getScreenName() + "@@@" + DateUtils.truncate(createdAt, calenderOption);;
                CassandraUtil.getInstance().insertCompositeColumns(columnFamily, rowKey, composite, key);
                System.out.println(rowKey + " : "  + key);
            }
        }
    }

    private static void insertTweet(String tweets, Status status) {
        //Map<String, String> tweetMap = demo.Tweet.getTweetMap(status);
       // demo.CassandraUtil.getInstance().insert(TWEETS_CF, String.valueOf(status.getId()), tweetMap);
    }

    public static void insertFriends(String screenName,List<String> frList) {
        Map<String, String> frMap = new HashMap<String, String>();
        for (String frName: frList) {
            frMap.put(frName, "");
        }
        CassandraUtil.getInstance().insert(USER_FRIENDS_CF, screenName, frMap);
    }

    public static void insertFollowers(String screenName,List<String> followersList) {
        Map<String, String> foMap = new HashMap<String, String>();
        for (String followerName: followersList) {
            foMap.put(followerName, "");
        }
        CassandraUtil.getInstance().insert(USER_FOLLOWERS_CF, screenName, foMap);
    }

    public static void insertUser(TwitterUser user) {
        Map<String, String> valueMap = TwitterUser.getUserMap(user);
        CassandraUtil.getInstance().insert(USERS_CF, user.getScreenName(), valueMap);
    }

    public static String generateRandText(int len){
        byte[] secretBytes = new byte[len*2];
        Random random = new SecureRandom();
        random.nextBytes(secretBytes);
        return DatatypeConverter.printBase64Binary(secretBytes).substring(0, len)
                .replace('/', 'A')
                .replace('\\', 'B')
                .replace('\'', 'C')
                .replace('"', 'D')
                .replace('?', 'E')
                .replace('%', 'F')
                .replace('+', 'G');
    }

    public static long generateRandId() {
        Random random = new SecureRandom();
        Long randNo = random.nextLong();
        return Math.abs(randNo);
    }
}
