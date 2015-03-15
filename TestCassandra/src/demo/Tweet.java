package demo;

import java.util.Date;
import java.util.Map;

/**
 * Author : asribalaji
 */
public class Tweet {
    String id;
    String createdAt;
    String screenName;
    String text;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String toString() {
        return "demo.Tweet {" +
                "id='" + id + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", screenName='" + screenName + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    protected static Tweet convertMaptoTweet(Map<String, String> tweetMap) {
        Tweet tweet = new Tweet();
        tweet.setId(tweetMap.get(CassandraUtil.getInstance().KEY));
        Long createdAt = Long.parseLong(tweetMap.get("createdAt"));
        //tweet.setCreatedAt(String.valueOf(createdAt));
        tweet.setCreatedAt(String.valueOf(new Date(createdAt)));
        tweet.setScreenName(tweetMap.get("screen_name"));
        tweet.setText(tweetMap.get("text"));
        return tweet;
    }
}
