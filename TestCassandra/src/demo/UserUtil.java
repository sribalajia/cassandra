package demo;/*
 * Copyright (c) 2010, Apigee Corporation.  All rights reserved.
 *  Apigee(TM) and the Apigee logo are trademarks or
 *  registered trademarks of Apigee Corp. or its subsidiaries.  All other
 *  trademarks are the property of their respective owners.
 */

import java.util.*;

/**
 * Author : asribalaji
 */
public class UserUtil {
    private static UserUtil instance = new UserUtil();

    public static UserUtil getInstance(){
        return instance;
    }

    public static void getFriendsForUser(String screenName) {
        List<TwitterUser> friends = new ArrayList<TwitterUser>();
        List<String> friendIds =  getFriendIdsForUser(screenName);
        for (String name : friendIds) {
            Map<String, String> userMap = CassandraUtil.getInstance().get(CassandraConstants.USERS_CF, name);
            if (userMap != null && userMap.size() > 0) {
                TwitterUser user = TwitterUser.convertMaptoUser(userMap);
                friends.add(user);
            }
        }
        for (TwitterUser user : friends) {
            System.out.println(user);
        }
    }

    public static void getFollowersForUser(String screenName) {
        List<TwitterUser> followers = new ArrayList<TwitterUser>();
        List<String> followerIds =  getFollowerIdsForUser(screenName);
        for (String name : followerIds) {
            Map<String, String> userMap = CassandraUtil.getInstance().get(CassandraConstants.USERS_CF, name);
            if (userMap != null && userMap.size() > 0) {
                TwitterUser user = TwitterUser.convertMaptoUser(userMap);
                followers.add(user);
            }
        }
        for (TwitterUser user : followers) {
            System.out.println(user);
        }
    }

    public static long getFriendsCountForUser(String screenName) {
        return CassandraUtil.getInstance().getCounterValue(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.FRIENDS);
    }

    public static long getFollowersCountUser(String screenName) {
        return CassandraUtil.getInstance().getCounterValue(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.FOLLOWERS);
    }


    public static void insertFollower(String screenName, String followerName) {
        Map<String, String> foMap = new HashMap<String, String>();
        foMap.put(followerName, "");
        CassandraUtil.getInstance().insert(CassandraConstants.USER_FOLLOWERS_CF, screenName, foMap);
        CassandraUtil.getInstance().incrementCounter(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.FOLLOWERS, 1);
    }

    public static void insertFollowers(String screenName,List<String> followersList) {
        Map<String, String> foMap = new HashMap<String, String>();
        for (String followerName: followersList) {
            foMap.put(followerName, "");
        }
        CassandraUtil.getInstance().insert(CassandraConstants.USER_FOLLOWERS_CF, screenName, foMap);
        CassandraUtil.getInstance().incrementCounter(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.FOLLOWERS, followersList.size());
    }

    public static void insertUser(TwitterUser user) {
        Map<String, String> valueMap = TwitterUser.getUserMap(user);
        CassandraUtil.getInstance().insert(CassandraConstants.USERS_CF, user.getScreenName(), valueMap);
    }

    public static List<String> getFriendIdsForUser(String screenName) {
        List<String> friendIds = new ArrayList<String>();
        Map<String, String> friendsMap = CassandraUtil.getInstance().get(CassandraConstants.USER_FRIENDS_CF, screenName);
        for (String key: friendsMap.keySet()) {
            friendIds.add(key);
        }
        return friendIds;
    }

    public static List<String> getFollowerIdsForUser(String screenName) {
        List<String> followerIds = new ArrayList<String>();
        Map<String, String> followersMap = CassandraUtil.getInstance().get(CassandraConstants.USER_FOLLOWERS_CF, screenName);
        for (String key: followersMap.keySet()) {
            followerIds.add(key);
        }
        return followerIds;
    }

    public static void insertFriend(String screenName, String friendName) {
        Map<String, String> frMap = new HashMap<String, String>();
        frMap.put(friendName, "");
        CassandraUtil.getInstance().insert(CassandraConstants.USER_FRIENDS_CF, screenName, frMap);
        CassandraUtil.getInstance().incrementCounter(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.FRIENDS, 1);
    }

    public static void insertFriends(String screenName,List<String> frList) {
        Map<String, String> frMap = new HashMap<String, String>();
        for (String frName: frList) {
            frMap.put(frName, "");
        }
        CassandraUtil.getInstance().insert(CassandraConstants.USER_FRIENDS_CF, screenName, frMap);
        CassandraUtil.getInstance().incrementCounter(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.FRIENDS, frList.size());

    }
}

