package demo;/*
 * Copyright (c) 2010, Apigee Corporation.  All rights reserved.
 *  Apigee(TM) and the Apigee logo are trademarks or
 *  registered trademarks of Apigee Corp. or its subsidiaries.  All other
 *  trademarks are the property of their respective owners.
 */

/**
 * Author : asribalaji
 */
public interface CassandraConstants {
    public static final String TWEETS_CF = "tweets";
    public static final String USERS_CF = "users";

    public static final String USER_FOLLOWERS_CF = "user_followers";
    public static final String USER_FRIENDS_CF = "user_friends";

    public static final String USER_TWEETS_DAY_CF = "user_tweets_day";
    public static final String USER_TWEETS_MONTH_CF = "user_tweets_month";
    public static final String USER_TWEETS_HOUR_CF = "user_tweets_hour";

    public static final String USER_COUNT_INFO_CF = "user_count_info";
    public static final String FOLLOWERS = "followers";
    public static final String FRIENDS = "friends";
    public static final String TWEETS = "tweets";
}
