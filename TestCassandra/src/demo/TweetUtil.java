package demo;/*
 * Copyright (c) 2010, Apigee Corporation.  All rights reserved.
 *  Apigee(TM) and the Apigee logo are trademarks or
 *  registered trademarks of Apigee Corp. or its subsidiaries.  All other
 *  trademarks are the property of their respective owners.
 */

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.*;

/**
 * Author : asribalaji
 */
public class TweetUtil {
    private static TweetUtil instance = new TweetUtil();

    public static TweetUtil getInstance(){
        return instance;
    }

    public static long getTweetsCountForUser(String screenName) {
        return CassandraUtil.getInstance().getCounterValue(CassandraConstants.USER_COUNT_INFO_CF, screenName, CassandraConstants.TWEETS);
    }

    public static void getTweetForUsersPerTimeline(String screenName, int month, int day, int hour) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        List<String> users = new ArrayList<String>();
        users.add(screenName);
        List<String> tweetIds = getTweetIdsForUsersPerTimeline(users, month, day, hour);
        for (String key: tweetIds) {
            Tweet tweet = Tweet.convertMaptoTweet(CassandraUtil.getInstance().get(CassandraConstants.TWEETS_CF, key));
            System.out.println(tweet);
        }
    }


    public static void getHomeTimeline(String screenName, int hours) {
        List<String> friends = UserUtil.getInstance().getFriendIdsForUser(screenName);
        List<Tweet> tweets = getTweetsForIds(getTweetIdsForUsers(friends, hours));
    }

    public static List<String> getTweetIdsForUsers(List<String> users, int hours) {
        final MultigetSliceQuery<String, Composite, String> query = HFactory.createMultigetSliceQuery(CassandraUtil.getInstance().getKeySpace(),
                StringSerializer.get(),
                CompositeSerializer.get(),
                StringSerializer.get());

        query.setColumnFamily(CassandraConstants.USER_TWEETS_DAY_CF);

        Date currentTime = new Date();
        Date startTime = new Date(115,currentTime.getMonth(),currentTime.getDate());
        Date endTime = new Date(115,currentTime.getMonth(),currentTime.getDate());
        //String key = user + "@@@" + startTime;
        List<String> rowKeys = new ArrayList<String>();
        for (String user: users) {
            rowKeys.add(user + "@@@" + startTime);
        }
        query.setKeys(rowKeys);
        endTime.setHours(currentTime.getHours() + currentTime.getMinutes());
        endTime.setMinutes(currentTime.getMinutes());
        startTime.setHours(currentTime.getHours()-hours);
        startTime.setMinutes(currentTime.getMinutes());
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

    public static List<String> getTweetIdsForUsersPerTimeline(List<String> users, int month, int date, int hour) {
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
            columnFamily = CassandraConstants.USER_TWEETS_MONTH_CF;
        }

        if (hour == 0)
            sHour = eHour = curTime.getHours();
        else {
            eHour = curTime.getHours();
            sHour = eHour - hour;
            columnFamily = CassandraConstants.USER_TWEETS_DAY_CF;
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

    public static List<Tweet> getAllTweetsForUser(String screenName) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        Map<String, String> columnMap = new HashMap<String, String>();
        columnMap.put("screen_name", screenName);
        List<String> tweetIds = CassandraUtil.getInstance().searchForKeysWithSearchMap(CassandraConstants.TWEETS_CF, columnMap, null, null, 0);
        for (String key: tweetIds) {
            Tweet tweet = Tweet.convertMaptoTweet(CassandraUtil.getInstance().get(CassandraConstants.TWEETS_CF, key));
            tweets.add(tweet);
            System.out.println(tweet);
        }
        return tweets;
    }

    public static List<Tweet> getTweetsForIds(List<String> ids) {
        List<Tweet> tweets = new ArrayList<Tweet>();
        for (String id : ids) {
            Tweet tweet = Tweet.convertMaptoTweet(CassandraUtil.getInstance().get(CassandraConstants.TWEETS_CF, id));
            Long createdAt = Long.parseLong(tweet.createdAt);
            tweet.setCreatedAt(String.valueOf(new Date(createdAt)));
            tweets.add(tweet);
            System.out.println(tweet);
        }
        return tweets;
    }



}
