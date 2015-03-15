package demo;

import me.prettyprint.cassandra.service.StringKeyIterator;
import me.prettyprint.hector.api.beans.Composite;
import org.apache.commons.lang.time.DateUtils;

import java.util.*;

/**
 * Author : asribalaji
 */

public class TwitterMain {
    static String SCREEN_NAME = "";

    public static void main(String[] args) throws InterruptedException {

        Scanner scan = new Scanner(System.in);
        System.out.println("/*\n" +
                " 0 - Load new data(users,friends,following,tweets) into database\n" +
                " 1 - Get homeline tweets for a user\n" +
                " 2 - Get User's tweets count\n" +
                " 3 - Get User's tweets\n" +
                " 4 - Get User's friends\n" +
                " 5 - Get User's followers\n" +
                " 6 - Get User's friend's count\n" +
                " 7 - Get User's follower's count\n" +
                " */");
        System.out.println("Enter the option : ");
        int input = Integer.parseInt(scan.next());
        System.out.println("Enter the user name : ");
        SCREEN_NAME = scan.next();
        switch (input) {
            case 0:
                loadData();
                break;
            case 1:
                // Get hometime tweets for user for last n hours
                TweetUtil.getHomeTimeline(SCREEN_NAME, 1);
                break;
            case 2:
                System.out.println(TweetUtil.getTweetsCountForUser(SCREEN_NAME));
                break;
            case 3:
                TweetUtil.getAllTweetsForUser(SCREEN_NAME);
                break;
            case 4:
                UserUtil.getFriendsForUser(SCREEN_NAME);
                break;
            case 5:
                UserUtil.getFollowersForUser(SCREEN_NAME);
                break;
            case 6:
                System.out.println(UserUtil.getFriendsCountForUser(SCREEN_NAME));
                break;
            case 7:
                System.out.println(UserUtil.getFollowersCountUser(SCREEN_NAME));
                break;
        }
        // Get hometime tweets for user for last n month/days/hours
        //TweetUtil.getTweetForUsersPerTimeline(SCREEN_NAME, 0, 0, 12);

    }

    private static void loadData() {
        createUsers();
        List<String> userIds = CassandraUtil.getInstance().listKeys(CassandraConstants.USERS_CF);
        createTweets(userIds);
        Random rand = new Random();
        SCREEN_NAME = userIds.get(rand.nextInt(userIds.size()+ 1));
        System.out.println("==========================");
        System.out.println("Selected UserName : " + SCREEN_NAME);
        System.out.println("==========================");
        createFriendsAndFollowersForUser(SCREEN_NAME);
        storeTweetsForTime(CassandraConstants.USER_TWEETS_MONTH_CF, Calendar.MONTH);
        storeTweetsForTime(CassandraConstants.USER_TWEETS_DAY_CF, Calendar.DAY_OF_MONTH);
        storeTweetsForTime(CassandraConstants.USER_TWEETS_HOUR_CF, Calendar.HOUR);
        System.out.println("==========================");
        System.out.println("Friends/Followers/Tweets added for UserName : " + SCREEN_NAME);
        System.out.println("==========================");

    }

    private static void createUsers() {
        for (int i=0; i < 100; i++) {
            TwitterUser newUser = new TwitterUser();
            String name = CommonUtil.generateRandText(8);
            newUser.setScreenName(name);
            newUser.setUserName(name.toUpperCase());
            newUser.setLocation(CommonUtil.generateRandText(8));
            newUser.setCreatedAt(new Date());
            newUser.setId(String.valueOf(CommonUtil.generateRandId()));
            UserUtil.insertUser(newUser);
            System.out.println(newUser);

        }
    }

    private static void createTweets(List<String> userIds) {
        for (String userName: userIds) {
            for (int j=0;j<10;j++) {
                Tweet tweet = new Tweet();
                tweet.setId(String.valueOf(CommonUtil.generateRandId()));
                tweet.setScreenName(userName);
                tweet.setCreatedAt(String.valueOf(new Date().getTime()));
                tweet.setText(CommonUtil.generateRandText(32));
                insertTweet(tweet);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Long createdAt = Long.parseLong(tweet.getCreatedAt());
                tweet.setCreatedAt(String.valueOf(new Date(createdAt)));
                System.out.println(tweet);
            }
        }
    }

    private static void createFriendsAndFollowersForUser(String screenName) {
        List<String> userIds = CassandraUtil.getInstance().listKeys(CassandraConstants.USERS_CF);
        userIds.remove(screenName);
        UserUtil.getInstance().insertFriends(screenName, userIds);
        UserUtil.getInstance().insertFollowers(screenName, userIds);
    }


    private static void storeTweetsForTime(String columnFamily, int calenderOption) {
        StringKeyIterator sk = new StringKeyIterator(CassandraUtil.getInstance().getKeySpace(), CassandraConstants.TWEETS_CF);
        Iterator<String> iterator = sk.iterator();
        List<String> resultList = new ArrayList<String>();
        while (iterator.hasNext()) {
            resultList.add(iterator.next());
        }
        if (resultList.size() > 0) {
            for (String key: resultList) {
                Map<String, String> tweetMap = CassandraUtil.getInstance().get(CassandraConstants.TWEETS_CF, key);
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

    private static void insertTweet(Tweet tweet) {
        Map<String, String> tweetMap = Tweet.convertTweetToMap(tweet);
        demo.CassandraUtil.getInstance().insert(CassandraConstants.TWEETS_CF, tweet.getId(), tweetMap);
    }
}
